package com.divisiblebyzero.hakea.processor

import java.io.File

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.solr.DispatchInputDocument
import com.divisiblebyzero.hakea.util.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.lib.{ ObjectId, Ref, Repository }
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk

import scala.concurrent.Future
import scala.util.matching.Regex

sealed trait FileIndexProcessorRequest

case class IndexFilesFor(project: Project, repository: Repository, refs: List[Ref]) extends FileIndexProcessorRequest

case class IndexFile(project: Project, repository: Repository, ref: Ref, objectId: ObjectId, path: String) extends FileIndexProcessorRequest

case class FileIndexingComplete(project: Project, repository: Repository, refs: List[Ref]) extends FileIndexProcessorRequest

class FileIndexProcessor(configuration: Configuration) extends Actor with Logging {
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

object FileIndexer {
  val PackageRegex = """^package\s+([\w\.]+)""".r
  val ImportRegex = """import\s+([\w\.\s,\{\}]+);?""".r
}

/*
 * TODO: Indexing files line-by-line may make it easier to deliver hits as snippets of code.
 *
 * Determining the best way to index files has been quite the challenge. Ideally files would be indexed line-by-line,
 * making it possible to display hits a snippets of code. It would also make it possible to filter results by line
 * number, which could have relevant use cases.
 */
class FileIndexer(configuration: Configuration) extends Actor with Logging {
  import context.dispatcher

  protected val inputDocumentDispatcher =
    context.actorFor("/user/hakeaProcessor/repositoryProcessor/indexProcessor/inputDocumentDispatcher")

  protected val indexProcessor = context.actorFor("/user/hakeaProcessor/repositoryProcessor/indexProcessor")

  def receive = {
    case IndexFile(project, repository, ref, objectId, path) => {
      Future {
        val inputDocument = new SolrInputDocument
        val loader = repository.open(objectId)
        val file = new File(path)
        val content = new String(loader.getCachedBytes)
        val id =
          "file::%s::%s::%s".format(project.name, file.getPath, ref.getObjectId.getName)

        inputDocument.addField("id", id)

        inputDocument.addField("project", project.name)

        inputDocument.addField("file_ref", ref.getName)

        parseFile(inputDocument, file, content)

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

  protected def parseFile(inputDocument: SolrInputDocument, file: File, content: String) {
    import FileIndexer._

    val filePath = file.getPath
    val fileName = file.getName
    val fileExtension = fileName.lastIndexOf(".") match {
      case i: Int if i > 0 => fileName.substring(i + 1)
      case _               => ""
    }

    inputDocument.addField("file_path", filePath)
    inputDocument.addField("file_name", fileName)
    inputDocument.addField("file_extension", fileExtension)
    inputDocument.addField("file_content", content)

    content.split("\n").foreach { line =>
      parseLine(PackageRegex, line).foreach(inputDocument.addField("file_packages", _))
      parseLine(ImportRegex, line).foreach(inputDocument.addField("file_imports", _))
    }
  }

  protected def parseLine(regex: Regex, line: String) =
    regex.findAllIn(line).matchData.map(_.group(1)).toList
}
