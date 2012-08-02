package com.divisiblebyzero.hakea.config

import com.divisiblebyzero.hakea.model.Project
import org.apache.solr.client.solrj.SolrServer

trait HakeaConfiguration {

  def home: String

  def repositoryHome: String

  def projects: List[HakeaProjectConfiguration]

  def solr: HakeaSolrConfiguration
}

trait HakeaProjectConfiguration {

  def name: String

  def uri: String

  def toProject: Project
}

trait HakeaSolrConfiguration {

  def url: String

  def queueSize: Int

  def threadCount: Int

  def toSolrServer: SolrServer
}
