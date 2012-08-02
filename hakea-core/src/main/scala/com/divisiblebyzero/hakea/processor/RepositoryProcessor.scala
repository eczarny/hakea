package com.divisiblebyzero.hakea.processor

import java.io.File

import akka.actor.{ Actor, Props }
import akka.dispatch.{ ExecutionContext, Future }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.util.RepositoryConversions._
import com.yammer.dropwizard.Logging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants.R_HEADS
import org.eclipse.jgit.lib.RefUpdate.Result.NO_CHANGE
import org.eclipse.jgit.lib.Repository

import scala.collection.JavaConversions._

sealed trait RepositoryProcessorRequest

case class CheckRepositoryForChanges(project: Project) extends RepositoryProcessorRequest

case class CloneRepository(project: Project) extends RepositoryProcessorRequest

class RepositoryProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val indexProcessor = context.actorOf(Props(new IndexProcessor(configuration)), "indexProcessor")

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  def receive = {
    case CheckRepositoryForChanges(project) => {
      val projectPath = project.path(configuration)
      val repository: Repository = projectPath

      if (!repository.getObjectDatabase.exists) {
        self ! CloneRepository(project)
      } else {
        val git = new Git(repository)

        Future {
          val fetchResult = git.fetch.call()

          fetchResult.getTrackingRefUpdates.filterNot(_.getResult == NO_CHANGE).map { trackingRefUpdate =>
            repository.getRef(trackingRefUpdate.getLocalName)
          }
        } onSuccess {
          case updatedRefs: List[_] if !updatedRefs.isEmpty => {
            indexProcessor ! IndexRepositoryFor(project, repository, updatedRefs)
          }
          case _ => {
            log.debug("The %s repository has not changed.".format(project.name))
          }
        }
      }
    }
    case CloneRepository(project) => {
      val projectPath = project.path(configuration)

      log.info("Cloning the %s repository at path: %s".format(project.name, projectPath))

      Future {
        val cloneCommand = Git.cloneRepository

        cloneCommand.setBare(true).setNoCheckout(true).setCloneAllBranches(true)

        cloneCommand.setURI(project.uri).setDirectory(new File(projectPath))

        cloneCommand.call()
      } onSuccess {
        case git: Git => {
          val repository = git.getRepository
          val refs = repository.getRefDatabase.getRefs(R_HEADS).values.toSeq

          indexProcessor ! IndexRepositoryFor(project, repository, refs.toList)
        }
      } onFailure {
        case e: Exception => e.printStackTrace()
      }
    }
  }
}
