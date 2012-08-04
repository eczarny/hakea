package com.divisiblebyzero.hakea.service.config

import com.divisiblebyzero.hakea.config.HakeaProjectConfiguration
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardProjectConfiguration extends HakeaProjectConfiguration {

  @NotEmpty
  @JsonProperty
  var name: String = "hakea"

  @NotEmpty
  @JsonProperty
  var uri: String = "git://github.com/eczarny/hakea.git"
}
