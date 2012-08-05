package com.divisiblebyzero.hakea.config

import java.io.File

import com.divisiblebyzero.hakea.model.Project
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer

trait HakeaConfiguration {

  def home: String

  def repositoryHome: String

  def projects: List[HakeaProjectConfiguration]

  def solr: HakeaSolrConfiguration

  protected def defaultHome(hakeaHome: String = "~/.hakea") =
    System.getProperty("hakea.home", hakeaHome).replace("~", userHome)

  protected def userHome = System.getProperty("user.home")

  protected def defaultRepositoryHome = home + File.separator + "repositories"
}

trait HakeaProjectConfiguration {

  def name: String

  def uri: String

  def toProject = Project(name, uri)
}

trait HakeaSolrConfiguration {

  def url: String

  def queueSize: Int

  def threadCount: Int

  def toSolrServer = new ConcurrentUpdateSolrServer(url, queueSize, threadCount)
}
