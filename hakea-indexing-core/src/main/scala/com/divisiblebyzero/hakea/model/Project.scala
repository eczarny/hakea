package com.divisiblebyzero.hakea.model

import java.io.File

import com.divisiblebyzero.hakea.config.Configuration

case class Project(name: String, uri: String) {

  def path(configuration: Configuration) =
    configuration.repositoryHome + File.separator + name
}
