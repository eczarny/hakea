package com.divisiblebyzero.hakea.indexing.processor

import akka.actor.{ Actor, Props }
import akka.dispatch.{ ExecutionContext, Future }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.indexing.solr.{ EnqueueInputDocument, InputDocumentQueue }
import com.yammer.dropwizard.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.lib.{ ObjectId, Ref, Repository }
import org.eclipse.jgit.revwalk.{ RevCommit, RevWalk }
import org.eclipse.jgit.treewalk.TreeWalk

sealed trait FileIndexProcessorRequest

case class IndexFilesForRefs(project: Project, repository: Repository, refs: List[Ref]) extends FileIndexProcessorRequest

case class IndexFilesAtRef(project: Project, repository: Repository, ref: Ref, revWalk: RevWalk, commit: RevCommit) extends FileIndexProcessorRequest

/*
  TODO: Indexing files line-by-line may make it easier to deliver hits as snippets of code.

  Determining the best way to index files has been quite the challenge. Ideally files would be indexed line-by-line,
  making it possible to display hits a snippets of code. It would also make it possible to filter results by line
  number, which could have relevant use cases.
 */
class FileIndexProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val inputDocumentQueue =
      context.actorOf(Props(new InputDocumentQueue(configuration)), "inputDocumentQueue")

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  def receive = {
    case IndexFilesForRefs(project, repository, refs) => {
      val revWalk = new RevWalk(repository)

      log.info("Indexing files for %s.".format(project.name))

      refs.foreach { ref =>
        self ! IndexFilesAtRef(project, repository, ref, revWalk, revWalk.parseCommit(ref.getObjectId))
      }
    }
    case IndexFilesAtRef(project, repository, ref, revWalk, commit) => {
      Future {
        val treeWalk = new TreeWalk(repository)

        treeWalk.setRecursive(true)

        treeWalk.addTree(commit.getTree)

        while (treeWalk.next()) {
          val objectId = treeWalk.getObjectId(0)

          if (objectId != ObjectId.zeroId) {
            val inputDocument = new SolrInputDocument
            val loader = repository.open(objectId)

            inputDocument.addField("id", "file::%s::%s".format(ref.getName, objectId.getName))

            inputDocument.addField("ref_s", ref.getName)
            inputDocument.addField("project_s", project.name)

            inputDocument.addField("file_path_s", new String(treeWalk.getPathString))
            inputDocument.addField("file_content_t", new String(loader.getCachedBytes))

            inputDocumentQueue ! EnqueueInputDocument(inputDocument)
          }
        }
      }
    }
  }
}
