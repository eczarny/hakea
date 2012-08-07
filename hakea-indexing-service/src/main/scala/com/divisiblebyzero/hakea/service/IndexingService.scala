package com.divisiblebyzero.hakea.service

import com.divisiblebyzero.hakea.service.config.ServiceConfiguration
import com.divisiblebyzero.hakea.service.managed.ManagedAkkaIndexer
import com.divisiblebyzero.hakea.service.resource.IndexingStatusResource
import com.yammer.dropwizard.ScalaService
import com.yammer.dropwizard.config.Environment

object IndexingService extends ScalaService[ServiceConfiguration]("hakea-indexing-service") {

  def initialize(configuration: ServiceConfiguration, environment: Environment) {
    environment.addResource(new IndexingStatusResource)

    environment.manage(new ManagedAkkaIndexer(configuration.hakea))
  }
}
