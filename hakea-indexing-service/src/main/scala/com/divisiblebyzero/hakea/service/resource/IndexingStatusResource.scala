package com.divisiblebyzero.hakea.service.resource

import javax.ws.rs._
import javax.ws.rs.core._

import com.yammer.metrics.annotation.Timed

@Path("/api/1.0/indexing/status")
@Produces(Array(MediaType.APPLICATION_JSON))
class IndexingStatusResource {

  @GET
  @Timed
  def status = Response.noContent.build()
}
