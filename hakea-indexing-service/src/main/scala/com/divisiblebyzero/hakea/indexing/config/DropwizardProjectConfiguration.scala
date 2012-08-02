package com.divisiblebyzero.hakea.indexing.config

import com.divisiblebyzero.hakea.config.HakeaProjectConfiguration
import com.divisiblebyzero.hakea.model.Project
import org.codehaus.jackson.annotate.JsonProperty
import org.hibernate.validator.constraints.NotEmpty

class DropwizardProjectConfiguration extends HakeaProjectConfiguration {

  @NotEmpty
  @JsonProperty
  var name: String = "hakea"

  @NotEmpty
  @JsonProperty
  var uri: String = "git://github.com/eczarny/hakea.git"

  def toProject = Project(name, uri)
}
