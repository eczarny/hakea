package com.divisiblebyzero.hakea.config

import java.io.File
import javax.validation.Valid

import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class HakeaConfiguration {

  @NotEmpty
  @JsonProperty
  var home: String = System.getProperty("hakea.home", "~/.hakea").replace("~", System.getProperty("user.home"))

  @NotEmpty
  @JsonProperty
  var repositoryHome: String = home + File.separator + "repositories"

  @Valid
  @JsonProperty
  val projects = List.empty[HakeaProjectConfiguration]

  @Valid
  @JsonProperty
  val solr = new HakeaSolrConfiguration
}
