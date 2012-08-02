package com.divisiblebyzero.hakea.indexing.config

import javax.validation.Valid

import com.yammer.dropwizard.config.Configuration
import org.codehaus.jackson.annotate.JsonProperty

class ServiceConfiguration extends Configuration {

  @Valid
  @JsonProperty
  val hakea = new DropwizardConfiguration
}
