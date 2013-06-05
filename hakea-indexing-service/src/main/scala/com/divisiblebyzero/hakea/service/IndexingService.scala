package com.divisiblebyzero.hakea.service

import com.divisiblebyzero.hakea.service.config.ServiceConfiguration
import com.divisiblebyzero.hakea.service.managed.ManagedAkkaIndexer
import com.divisiblebyzero.hakea.service.resource.IndexingStatusResource
import com.yammer.dropwizard.ScalaService
import com.yammer.dropwizard.bundles.ScalaBundle
import com.yammer.dropwizard.config.{ Bootstrap, Environment }

object IndexingService extends ScalaService[ServiceConfiguration] {

  def initialize(bootstrap: Bootstrap[ServiceConfiguration]) {
    bootstrap.setName("hakea-indexing-service")

    bootstrap.addBundle(new ScalaBundle)
  }

  def run(configuration: ServiceConfiguration, environment: Environment) {
    environment.addResource(new IndexingStatusResource)

    environment.manage(new ManagedAkkaIndexer(configuration.hakea))
  }
}
