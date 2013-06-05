package com.divisiblebyzero.hakea.service.managed

import com.divisiblebyzero.hakea.HakeaIndexer
import com.divisiblebyzero.hakea.config.Configuration

import scala.concurrent.duration._

class ManagedAkkaIndexer(configuration: Configuration) extends ManagedIndexer(configuration) {
  protected val indexer = new HakeaIndexer(configuration)

  override def start() {
    indexer.scheduleIndexer(5 seconds, 10 minutes)
  }

  override def stop() {
    indexer.shutdown()
  }
}
