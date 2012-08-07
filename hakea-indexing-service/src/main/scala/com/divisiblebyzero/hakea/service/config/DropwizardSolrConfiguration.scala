package com.divisiblebyzero.hakea.service.config

import com.divisiblebyzero.hakea.config.SolrConfiguration
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardSolrConfiguration extends SolrConfiguration {

  @NotEmpty
  @JsonProperty
  var url: String = "http://localhost:8983/solr"

  @JsonProperty
  var queueSize: Int = 25

  @JsonProperty
  var threadCount: Int = 4
}
