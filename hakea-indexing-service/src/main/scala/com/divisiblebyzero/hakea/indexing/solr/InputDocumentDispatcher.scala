package com.divisiblebyzero.hakea.indexing.solr

import java.util.Date

import akka.actor.Actor
import akka.dispatch.{ ExecutionContext, Future }
import akka.util.duration._

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.yammer.dropwizard.Logging
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument

import scala.collection.JavaConverters._

sealed trait InputDocumentDispatcherRequest

case class DispatchInputDocuments(inputDocuments: Seq[SolrInputDocument]) extends InputDocumentDispatcherRequest

case object CommitInputDocuments extends InputDocumentDispatcherRequest

class InputDocumentDispatcher(configuration: HakeaConfiguration) extends Actor with Logging {
  protected lazy val solrServer = configuration.solr.toSolrServer
  protected var previousCommitTime = currentTime
  protected var commitRequired = false

  implicit private val executionContext = ExecutionContext.defaultExecutionContext(context.system)

  context.system.scheduler.schedule(5 minutes, 2 minutes) {
    self ! CommitInputDocuments
  }

  def receive = {
    case DispatchInputDocuments(inputDocuments) => {
      val replyTo = sender

      log.debug("Dispatching %s input document(s) to Solr.".format(inputDocuments.size))

      Future {
        solrServer.add(inputDocuments.asJava)
      } onSuccess {
        case response: UpdateResponse if response.getStatus == 0 => {
          log.debug("Successfully dispatched %s input document(s).".format(inputDocuments.size))

          commitRequired = true

          if ((currentTime - previousCommitTime) > 30L) {
            self ! CommitInputDocuments
          }
        }
        case _ => {
          log.error("Unable to disatpch input documents, placing back in queue.")

          replyTo ! EnqueueInputDocuments(inputDocuments)
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
