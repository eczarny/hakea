package com.divisiblebyzero.hakea.indexing.solr

import akka.actor.Actor
import akka.dispatch.{ ExecutionContext, Future }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.yammer.dropwizard.Logging
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

sealed trait InputDocumentDispatcherRequest

case class DispatchInputDocument(inputDocument: SolrInputDocument) extends InputDocumentDispatcherRequest

case object CommitInputDocuments extends InputDocumentDispatcherRequest

class InputDocumentDispatcher(configuration: HakeaConfiguration) extends Actor with Logging {
  protected lazy val solrServer = configuration.solr.toSolrServer

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  def receive = {
    case DispatchInputDocument(inputDocument) => {
      Future {
        solrServer.add(inputDocument)
      } onSuccess {
        case response: UpdateResponse if response.getStatus == 0 => {
          log.debug("Successfully dispatched input document.")
        }
        case _ => {
          log.error("Unable to disatpch input document.")
        }
      }
    }
    case CommitInputDocuments => {
      Future {
        solrServer.commit()
      } onSuccess {
        case response: UpdateResponse if response.getStatus == 0 => {
          log.info("Successfully committed input documents.")
        }
      }
    }
  }
}
