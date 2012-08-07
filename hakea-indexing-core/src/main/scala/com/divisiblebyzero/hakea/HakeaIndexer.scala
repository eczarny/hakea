package com.divisiblebyzero.hakea

import akka.actor.{ ActorSystem, Props }
import akka.util.Duration

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.processor.{ HakeaProcessor, StartIndexing }
import com.divisiblebyzero.hakea.util.Logging

class HakeaIndexer(configuration: Configuration) extends Logging {
  protected val system = ActorSystem("hakea")

  protected val hakeaProcessor =
    system.actorOf(Props(new HakeaProcessor(configuration)), "hakeaProcessor")

  def index(projects: Project*) {
    hakeaProcessor ! StartIndexing(projects.toList)
  }

  def scheduleIndexer(initialDelay: Duration, frequency: Duration) {
    val projects = configuration.projects.map(_.toProject)

    log.info("Starting up Hakea with projects: %s".format(projects.map(_.name)))

    system.scheduler.schedule(initialDelay, frequency) {
      index(projects: _*)
    }
  }

  def shutdown() {
    log.info("Shutting down Hakea...")

    system.shutdown()
  }
}
