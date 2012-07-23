package com.divisiblebyzero.hakea.indexing.managed

import akka.actor.{ ActorSystem, Props }
import akka.util.duration._

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.indexing.processor.{ ProjectProcessor, StartIndexing, StopIndexing }
import com.yammer.dropwizard.Logging

class ManagedAkkaIndexer(configuration: HakeaConfiguration) extends ManagedIndexer(configuration) with Logging {
  protected val system = ActorSystem("hakea")

  protected val projectProcessor = system.actorOf(Props(new ProjectProcessor(configuration)), "projectProcessor")

  override def start() {
    val projects = configuration.projects.map(_.toProject)

    log.info("Starting up Hakea with projects: %s".format(projects.map(_.name)))

    system.scheduler.schedule(5 seconds, 5 minutes) {
      projectProcessor ! StartIndexing(projects)
    }
  }

  override def stop() {
    projectProcessor ! StopIndexing
  }
}
