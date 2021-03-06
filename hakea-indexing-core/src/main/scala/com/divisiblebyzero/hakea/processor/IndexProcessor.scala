package com.divisiblebyzero.hakea.processor

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.solr.{ CommitInputDocuments, InputDocumentDispatcher }
import com.divisiblebyzero.hakea.util.Logging
import org.eclipse.jgit.lib.{ Ref, Repository }

sealed trait IndexProcessorRequest

case class IndexRepositoryFor(project: Project, repository: Repository, refs: List[Ref]) extends IndexProcessorRequest

case class FinishedIndexingCommitsFor(project: Project, repository: Repository, refs: List[Ref]) extends IndexProcessorRequest

case class FinishedIndexingFilesFor(project: Project, repository: Repository, refs: List[Ref]) extends IndexProcessorRequest

class IndexProcessor(configuration: Configuration) extends Actor with Logging {
  protected val commitIndexProcessor =
    context.actorOf(Props(new CommitIndexProcessor(configuration)), "commitIndexProcessor")

  protected val fileIndexProcessor =
    context.actorOf(Props(new FileIndexProcessor(configuration)), "fileIndexProcessor")

  protected val inputDocumentDispatcher =
    context.actorOf(Props(new InputDocumentDispatcher(configuration))
                      .withDispatcher("hakea.dispatcher.input-document-dispatcher"), "inputDocumentDispatcher")

  def receive = {
    case IndexRepositoryFor(project, repository, refs) => {
      commitIndexProcessor ! IndexCommitsFor(project, repository, refs)
    }
    case FinishedIndexingCommitsFor(project, repository, refs) => {
      log.info("Finished indexing the commit history of %s.".format(project.name))

      fileIndexProcessor ! IndexFilesFor(project, repository, refs)
    }
    case FinishedIndexingFilesFor(project, repository, refs) => {
      log.info("Finished indexing files for %s.".format(project.name))

      inputDocumentDispatcher ! CommitInputDocuments
    }
  }
}
