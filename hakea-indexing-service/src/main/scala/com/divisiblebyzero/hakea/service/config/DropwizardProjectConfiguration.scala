package com.divisiblebyzero.hakea.service.config

import com.divisiblebyzero.hakea.config.ProjectConfiguration
import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardProjectConfiguration extends ProjectConfiguration {

  @NotEmpty
  @JsonProperty
  var name: String = "hakea"

  @NotEmpty
  @JsonProperty
  var uri: String = "git://github.com/eczarny/hakea.git"
}
