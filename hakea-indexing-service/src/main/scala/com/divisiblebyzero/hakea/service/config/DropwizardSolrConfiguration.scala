package com.divisiblebyzero.hakea.service.config

import com.divisiblebyzero.hakea.config.HakeaSolrConfiguration
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardSolrConfiguration extends HakeaSolrConfiguration {

  @NotEmpty
  @JsonProperty
  var url: String = "http://localhost:8983/solr"

  @JsonProperty
  var queueSize: Int = 10

  @JsonProperty
  var threadCount: Int = 2
}
