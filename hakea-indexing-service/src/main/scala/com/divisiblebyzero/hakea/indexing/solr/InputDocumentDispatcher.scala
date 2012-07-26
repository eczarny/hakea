package com.divisiblebyzero.hakea.indexing.solr

import java.util.Date

import akka.actor.Actor
import akka.dispatch.{ ExecutionContext, Future }
import akka.util.duration._

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.yammer.dropwizard.Logging
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

sealed trait InputDocumentDispatcherRequest

case class DispatchInputDocument(inputDocument: SolrInputDocument) extends InputDocumentDispatcherRequest

case object CommitInputDocuments extends InputDocumentDispatcherRequest

class InputDocumentDispatcher(configuration: HakeaConfiguration) extends Actor with Logging {
  protected lazy val solrServer = configuration.solr.toSolrServer
  protected var previousCommitTime = currentTime
  protected var commitRequired = false

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  context.system.scheduler.schedule(5 minutes, 10 minutes) {
    self ! CommitInputDocuments
  }

  def receive = {
    case DispatchInputDocument(inputDocument) => {
      log.debug("Dispatching input document to Solr.")

      Future {
        solrServer.add(inputDocument)
      } onSuccess {
        case response: UpdateResponse if response.getStatus == 0 => {
          log.debug("Successfully dispatched input document.")

          commitRequired = true
        }
        case _ => {
          log.error("Unable to disatpch input document.")
        }
      }
    }
    case CommitInputDocuments => {
      if (commitRequired) {
        Future {
          solrServer.commit()
        } onSuccess {
          case response: UpdateResponse if response.getStatus == 0 => {
            log.info("Successfully committed input documents.")

            previousCommitTime = currentTime

            commitRequired = false
          }
        }
      }
    }
  }

  protected def currentTime = new Date().getTime / 1000
}
