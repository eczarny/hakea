package com.divisiblebyzero.hakea.model

import java.io.File

import com.divisiblebyzero.hakea.config.HakeaConfiguration

case class Project(name: String, uri: String) {

  def path(configuration: HakeaConfiguration) = configuration.repositoryHome + File.separator + name
}
