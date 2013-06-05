package com.divisiblebyzero.hakea.service.config

import javax.validation.Valid

import com.yammer.dropwizard.config.Configuration
import com.fasterxml.jackson.annotation.JsonProperty

class ServiceConfiguration extends Configuration {

  @Valid
  @JsonProperty
  val hakea = new DropwizardConfiguration
}
