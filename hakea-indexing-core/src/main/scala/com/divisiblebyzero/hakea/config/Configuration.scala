package com.divisiblebyzero.hakea.config

import java.io.File

import com.divisiblebyzero.hakea.model.Project
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer

trait Configuration {

  def home: String

  def repositoryHome: String

  def projects: List[ProjectConfiguration]

  def solr: SolrConfiguration

  protected def defaultHome(hakeaHome: String = "~/.hakea") =
    System.getProperty("hakea.home", hakeaHome).replace("~", userHome)

  protected def userHome = System.getProperty("user.home")

  protected def defaultRepositoryHome = home + File.separator + "repositories"
}

trait ProjectConfiguration {

  def name: String

  def uri: String

  def toProject = Project(name, uri)
}

trait SolrConfiguration {

  def url: String

  def queueSize: Int

  def threadCount: Int

  def toSolrServer = new ConcurrentUpdateSolrServer(url, queueSize, threadCount)
}
