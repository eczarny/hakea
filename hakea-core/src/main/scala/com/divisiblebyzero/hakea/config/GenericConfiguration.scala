package com.divisiblebyzero.hakea.config

import com.typesafe.config.Config

import scala.collection.JavaConversions._

class GenericConfiguration(config: Config) extends HakeaConfiguration {

  val home = defaultHome()

  val repositoryHome = defaultRepositoryHome

  lazy val projects =
    config.getConfigList("projects").map(new GenericProjectConfiguration(_)).toList

  lazy val solr = new GenericSolrConfiguration(config.getConfig("solr"))
}

class GenericProjectConfiguration(config: Config) extends HakeaProjectConfiguration {

  val name = config.getString("name")

  val uri = config.getString("uri")
}

class GenericSolrConfiguration(config: Config) extends HakeaSolrConfiguration {

  val url = config.getString("url")

  val queueSize = config.getInt("queueSize")

  val threadCount = config.getInt("threadCount")
}
