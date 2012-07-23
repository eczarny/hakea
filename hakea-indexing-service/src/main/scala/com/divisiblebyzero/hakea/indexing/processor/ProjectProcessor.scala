package com.divisiblebyzero.hakea.indexing.processor

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.yammer.dropwizard.Logging

sealed trait ProjectProcessorRequest

case class StartIndexing(projects: List[Project]) extends ProjectProcessorRequest

case object StopIndexing extends ProjectProcessorRequest

class ProjectProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val repositoryProcessor = context.actorOf(Props(new RepositoryProcessor(configuration)), "repositoryProcessor")

  def receive = {
    case StartIndexing(projects) => projects.foreach { project =>
      repositoryProcessor ! CheckRepositoryForChanges(project)
    }
    case StopIndexing => {
      log.info("Shutting down all Hakea processors...")
    }
  }
}
