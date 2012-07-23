package com.divisiblebyzero.hakea.indexing.solr

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.yammer.dropwizard.Logging
import org.apache.solr.common.SolrInputDocument

import scala.collection.mutable

sealed trait InputDocumentQueueRequest

case class EnqueueInputDocument(inputDocument: SolrInputDocument) extends InputDocumentQueueRequest

case class EnqueueInputDocuments(inputDocuments: Seq[SolrInputDocument]) extends InputDocumentQueueRequest

class InputDocumentQueue(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val inputDocumentDispatcher =
    context.actorOf(Props(new InputDocumentDispatcher(configuration)), "inputDocumentDispatcher")

  protected val inputDocumentsQueue = mutable.Queue.empty[SolrInputDocument]

  def receive = {
    case EnqueueInputDocument(inputDocument) => {
      log.debug("Adding input document to the queue: %s".format(inputDocument.getFieldNames))

      inputDocumentsQueue.enqueue(inputDocument)

      if (inputDocumentsQueue.size >= configuration.solr.batchSize) {
        inputDocumentDispatcher ! DispatchInputDocuments(inputDocumentsQueue.dequeueAll(!_.isEmpty))
      }
    }
    case EnqueueInputDocuments(inputDocuments) => {
      log.debug("Adding %s input document(s) to the queue.".format(inputDocuments.size))

      inputDocumentsQueue.enqueue(inputDocuments: _*)
    }
  }
}
