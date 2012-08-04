package com.divisiblebyzero.hakea.service.config

import javax.validation.Valid

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardConfiguration extends HakeaConfiguration {

  @NotEmpty
  @JsonProperty
  var home: String = defaultHome()

  @NotEmpty
  @JsonProperty
  var repositoryHome: String = defaultRepositoryHome

  @Valid
  @JsonProperty
  val projects = List.empty[DropwizardProjectConfiguration]

  @Valid
  @JsonProperty
  val solr = new DropwizardSolrConfiguration
}
