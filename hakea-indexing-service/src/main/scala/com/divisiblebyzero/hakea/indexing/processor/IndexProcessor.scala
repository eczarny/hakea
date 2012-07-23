package com.divisiblebyzero.hakea.indexing.processor

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.yammer.dropwizard.Logging
import org.eclipse.jgit.lib.{ Ref, Repository }

sealed trait IndexProcessorRequest

case class IndexRepository(project: Project, repository: Repository, refs: List[Ref]) extends IndexProcessorRequest

class IndexProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val commitIndexProcessor =
    context.actorOf(Props(new CommitIndexProcessor(configuration)), "commitIndexProcessor")

  protected val fileIndexProcessor =
    context.actorOf(Props(new FileIndexProcessor(configuration)), "fileIndexProcessor")

  def receive = {
    case IndexRepository(project, repository, refs) => {
      refs.foreach { ref =>
        commitIndexProcessor ! IndexCommitsAtRef(project, repository, ref)
      }

      fileIndexProcessor ! IndexFilesForRefs(project, repository, refs)
    }
  }
}
