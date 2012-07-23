package com.divisiblebyzero.hakea.config

import org.apache.solr.client.solrj.impl.HttpSolrServer
import org.hibernate.validator.constraints.NotEmpty
import org.codehaus.jackson.annotate.JsonProperty

class HakeaSolrConfiguration {

  @NotEmpty
  @JsonProperty
  var url: String = "http://localhost:8983/solr"

  @JsonProperty
  var batchSize: Int = 100

  def toSolrServer = new HttpSolrServer(url)
}
