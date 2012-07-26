package com.divisiblebyzero.hakea.config

import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer
import org.hibernate.validator.constraints.NotEmpty
import org.codehaus.jackson.annotate.JsonProperty

class HakeaSolrConfiguration {

  @NotEmpty
  @JsonProperty
  var url: String = "http://localhost:8983/solr"

  @JsonProperty
  var queueSize: Int = 10

  @JsonProperty
  var threadCount: Int = 2

  def toSolrServer: SolrServer = new ConcurrentUpdateSolrServer(url, queueSize, threadCount)
}
