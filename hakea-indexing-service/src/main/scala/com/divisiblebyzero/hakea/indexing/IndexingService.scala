package com.divisiblebyzero.hakea.indexing

import com.divisiblebyzero.hakea.indexing.config.ServiceConfiguration
import com.divisiblebyzero.hakea.indexing.managed.ManagedAkkaIndexer
import com.divisiblebyzero.hakea.indexing.resource.IndexingStatusResource
import com.yammer.dropwizard.{ Logging, ScalaService }
import com.yammer.dropwizard.config.Environment

object IndexingService extends ScalaService[ServiceConfiguration]("hakea-indexing-service") with Logging {

  def initialize(configuration: ServiceConfiguration, environment: Environment) {
    environment.addResource(new IndexingStatusResource)

    environment.manage(new ManagedAkkaIndexer(configuration.hakea))
  }
}
