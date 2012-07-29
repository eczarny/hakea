package com.divisiblebyzero.hakea.indexing.processor

import akka.actor.{ Actor, Props }
import akka.dispatch.{ ExecutionContext, Future }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.indexing.solr.{ DispatchInputDocument, InputDocumentDispatcher }
import com.yammer.dropwizard.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.lib.{ ObjectId, Ref, Repository }
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk

sealed trait FileIndexProcessorRequest

case class IndexFilesFor(project: Project, repository: Repository, refs: List[Ref]) extends FileIndexProcessorRequest

case class IndexFile(project: Project, repository: Repository, ref: Ref, objectId: ObjectId, path: String) extends FileIndexProcessorRequest

case class FileIndexingComplete(project: Project, repository: Repository, refs: List[Ref]) extends FileIndexProcessorRequest

class FileIndexProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val fileIndexer = context.actorOf(Props(new FileIndexer(configuration)), "fileIndexer")

  def receive = {
    case IndexFilesFor(project, repository, refs) => {
      val revWalk = new RevWalk(repository)

      log.info("Indexing files for %s.".format(project.name))

      refs.foreach { ref =>
        val commit = revWalk.parseCommit(ref.getObjectId)
        val treeWalk = new TreeWalk(repository)

        treeWalk.setRecursive(true)

        treeWalk.addTree(commit.getTree)

        while (treeWalk.next()) {
          val objectId = treeWalk.getObjectId(0)

          if (objectId != ObjectId.zeroId) {
            fileIndexer ! IndexFile(project, repository, ref, objectId, treeWalk.getPathString)
          }
        }
      }

      fileIndexer ! FileIndexingComplete(project, repository, refs)
    }
  }
}

/*
 * TODO: Indexing files line-by-line may make it easier to deliver hits as snippets of code.
 *
 * Determining the best way to index files has been quite the challenge. Ideally files would be indexed line-by-line,
 * making it possible to display hits a snippets of code. It would also make it possible to filter results by line
 * number, which could have relevant use cases.
 */
class FileIndexer(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val inputDocumentDispatcher =
    context.actorFor("/user/projectProcessor/repositoryProcessor/indexProcessor/inputDocumentDispatcher")

  protected val indexProcessor = context.actorFor("/user/projectProcessor/repositoryProcessor/indexProcessor")

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  def receive = {
    case IndexFile(project, repository, ref, objectId, path) => {
      Future {
        val inputDocument = new SolrInputDocument
        val loader = repository.open(objectId)

        inputDocument.addField("id", "file::%s::%s".format(ref.getName, objectId.getName))

        inputDocument.addField("refs", ref.getName)
        inputDocument.addField("project", project.name)

        inputDocument.addField("file_path", path)
        inputDocument.addField("file_content", new String(loader.getCachedBytes))

        inputDocument
      } onSuccess {
        case inputDocument: SolrInputDocument => {
          inputDocumentDispatcher ! DispatchInputDocument(inputDocument)
        }
      }
    }
    case FileIndexingComplete(project, repository, refs) => {
      indexProcessor ! FinishedIndexingFilesFor(project, repository, refs)
    }
  }
}
