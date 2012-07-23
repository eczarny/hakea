package com.divisiblebyzero.hakea.indexing.processor

import java.io.ByteArrayOutputStream

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.indexing.solr.{ EnqueueInputDocument, InputDocumentQueue }
import com.yammer.dropwizard.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.diff._
import org.eclipse.jgit.lib.{ Ref, Repository }
import org.eclipse.jgit.revwalk.{ RevCommit, RevWalk }

import scala.collection.JavaConversions._

sealed trait CommitIndexProcessorRequest

case class IndexCommitsAtRef(project: Project, repository: Repository, ref: Ref) extends CommitIndexProcessorRequest

class CommitIndexProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val inputDocumentQueue =
    context.actorOf(Props(new InputDocumentQueue(configuration)), "inputDocumentQueue")

  def receive = {
    case IndexCommitsAtRef(project, repository, ref) => {
      val walk = new RevWalk(repository)

      log.info("Indexing the commit history of %s at: %s".format(project.name, ref.getName))

      walk.markStart(walk.parseCommit(ref.getObjectId))

      walk.foreach { commit =>
        inputDocumentQueue ! EnqueueInputDocument(commitToInputDocument(project, repository, ref, commit, walk))
      }
    }
  }

  protected def commitToInputDocument(project: Project, repository: Repository, ref: Ref, commit: RevCommit, walk: RevWalk) = {
    val inputDocument = new SolrInputDocument

    inputDocument.addField("id", "commit::%s".format(commit.getId.name))

    inputDocument.addField("ref_s", ref.getName)
    inputDocument.addField("project_s", project.name)

    inputDocument.addField("commit_author_name_t", commit.getAuthorIdent.getName)
    inputDocument.addField("commit_author_email_s", commit.getAuthorIdent.getEmailAddress)
    inputDocument.addField("commit_author_date_dt", commit.getAuthorIdent.getWhen)
    inputDocument.addField("commit_committer_name_t", commit.getCommitterIdent.getName)
    inputDocument.addField("commit_committer_email_s", commit.getCommitterIdent.getEmailAddress)
    inputDocument.addField("commit_committer_date_dt", commit.getCommitterIdent.getWhen)
    inputDocument.addField("commit_full_message_en", commit.getFullMessage)
    inputDocument.addField("commit_short_message_en", commit.getShortMessage)

    commit.getFooterLines.filterNot(_.getValue.isEmpty).foreach { footerLine =>
      inputDocument.addField("commit_footer_line_%s_t".format(footerLine.getKey.toLowerCase), footerLine.getValue)
    }

    parseDiffs(repository, commit, walk, inputDocument)

    inputDocument
  }

  /*
    TODO: Index diffs line-by-line, noting additions and deletions.

    There is a lot of work that can be done here. The diff format (broken down into files and chunks) lends itself well
    to indexing. It would be great if Solr could not only index diffs line-by-line (noting additions and deletions),
    but also files and chunks the changes occurred in.
   */
  protected def parseDiffs(repository: Repository, commit: RevCommit, walk: RevWalk, inputDocument: SolrInputDocument) {
    if (commit.getParentCount > 0) {
      val parent = walk.parseCommit(commit.getParent(0).getId)
      val diffOutputStream = new ByteArrayOutputStream
      val diffFormatter = new DiffFormatter(diffOutputStream)

      diffFormatter.setRepository(repository)
      diffFormatter.setDiffComparator(RawTextComparator.DEFAULT)
      diffFormatter.setDetectRenames(true)

      diffFormatter.format(parent.getTree, commit.getTree)

      inputDocument.addField("commit_diff_t", new String(diffOutputStream.toByteArray))
    }
  }
}
