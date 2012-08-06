package com.divisiblebyzero.hakea.service.managed

import akka.actor.{ ActorSystem, Props }
import akka.util.duration._

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.processor.{ HakeaProcessor, StartIndexing }
import com.yammer.dropwizard.Logging

class ManagedAkkaIndexer(configuration: Configuration) extends ManagedIndexer(configuration) with Logging {
  protected val system = ActorSystem("hakea")

  protected val hakeaProcessor =
    system.actorOf(Props(new HakeaProcessor(configuration)), "hakeaProcessor")

  override def start() {
    val projects = configuration.projects.map(_.toProject)

    log.info("Starting up Hakea with projects: %s".format(projects.map(_.name)))

    system.scheduler.schedule(5 seconds, 10 minutes) {
      hakeaProcessor ! StartIndexing(projects)
    }
  }

  override def stop() {
    log.info("Shutting down Hakea...")

    system.shutdown()
  }
}
