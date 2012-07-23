package com.divisiblebyzero.hakea.search

import com.divisiblebyzero.hakea.config.ServiceConfiguration
import com.divisiblebyzero.hakea.search.resource.SearchResource
import com.yammer.dropwizard.{ Logging, ScalaService }
import com.yammer.dropwizard.bundles.AssetsBundle
import com.yammer.dropwizard.config.Environment

object SearchService extends ScalaService[ServiceConfiguration]("hakea-search-service") with Logging {
  addBundle(new AssetsBundle)

  def initialize(configuration: ServiceConfiguration, environment: Environment) {
    val solrServer = configuration.hakea.solr.toSolrServer

    environment.addResource(new SearchResource(solrServer))
  }
}
