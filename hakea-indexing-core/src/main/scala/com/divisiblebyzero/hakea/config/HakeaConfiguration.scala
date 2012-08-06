package com.divisiblebyzero.hakea.config

import com.typesafe.config.{ Config, ConfigFactory }

import scala.collection.JavaConversions._

object HakeaConfiguration {

  def load() = new HakeaConfiguration(ConfigFactory.load().getConfig("hakea"))
}

class HakeaConfiguration(config: Config) extends Configuration {

  val home = defaultHome()

  val repositoryHome = defaultRepositoryHome

  lazy val projects =
    config.getConfigList("projects").map(new HakeaProjectConfiguration(_)).toList

  lazy val solr = new HakeaSolrConfiguration(config.getConfig("solr"))
}

class HakeaProjectConfiguration(config: Config) extends ProjectConfiguration {

  val name = config.getString("name")

  val uri = config.getString("uri")
}

class HakeaSolrConfiguration(config: Config) extends SolrConfiguration {

  val url = config.getString("url")

  val queueSize = config.getInt("queueSize")

  val threadCount = config.getInt("threadCount")
}
