package com.divisiblebyzero.hakea.indexing.config

import java.io.File
import javax.validation.Valid

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardConfiguration extends HakeaConfiguration {

  @NotEmpty
  @JsonProperty
  var home: String = System.getProperty("hakea.home", "~/.hakea").replace("~", System.getProperty("user.home"))

  @NotEmpty
  @JsonProperty
  var repositoryHome: String = home + File.separator + "repositories"

  @Valid
  @JsonProperty
  val projects = List.empty[DropwizardProjectConfiguration]

  @Valid
  @JsonProperty
  val solr = new DropwizardSolrConfiguration
}
