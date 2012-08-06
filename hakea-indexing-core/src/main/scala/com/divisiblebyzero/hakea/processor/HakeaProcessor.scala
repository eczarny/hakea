package com.divisiblebyzero.hakea.processor

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.util.Logging

sealed trait HakeaProcessorRequest

case class StartIndexing(projects: List[Project]) extends HakeaProcessorRequest

class HakeaProcessor(configuration: Configuration) extends Actor with Logging {
  protected val repositoryProcessor = context.actorOf(Props(new RepositoryProcessor(configuration)), "repositoryProcessor")

  def receive = {
    case StartIndexing(projects) => projects.foreach { project =>
      repositoryProcessor ! CheckRepositoryForChanges(project)
    }
  }
}
