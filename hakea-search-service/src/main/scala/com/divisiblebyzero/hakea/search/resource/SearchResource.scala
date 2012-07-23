package com.divisiblebyzero.hakea.search.resource

import javax.ws.rs._
import javax.ws.rs.core._

import com.yammer.metrics.annotation.Timed
import org.apache.solr.client.solrj.{ SolrQuery, SolrServer }

@Path("/api/1.0/search")
@Produces(Array(MediaType.APPLICATION_JSON))
class SearchResource(solrServer: SolrServer) {

  @GET
  @Timed
  def search(@QueryParam("q") query: String, @QueryParam("fq") filterQueries: Seq[String]) = {
    Response.ok(doSearch(query, filterQueries)).build()
  }

  protected def doSearch(query: String, filterQueries: Seq[String]) =
    solrServer.query(new SolrQuery().setQuery(query).setFilterQueries(filterQueries: _*)).getResults
}
