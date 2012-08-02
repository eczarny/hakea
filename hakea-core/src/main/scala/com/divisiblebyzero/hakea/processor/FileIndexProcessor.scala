package com.divisiblebyzero.hakea.processor

import java.io.File

import akka.actor.{ Actor, Props }
import akka.dispatch.{ ExecutionContext, Future }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.solr.DispatchInputDocument
import com.divisiblebyzero.hakea.util.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.lib.{ ObjectId, Ref, Repository }
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk

import scala.util.matching.Regex

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

object FileIndexer {
  val PackageRegex = """^package\s+([\w\.\d]+)""".r
  val ImportRegex = """^import\s+(.+)[\n|;]""".r
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
        val file = new File(path)
        val content = new String(loader.getCachedBytes)

        inputDocument.addField("id", "file::%s::%s".format(ref.getName, objectId.getName))

        inputDocument.addField("project", project.name)

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

    val filePath = file.getAbsolutePath
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
    regex.findFirstMatchIn(line).map(_.group(1))
}
