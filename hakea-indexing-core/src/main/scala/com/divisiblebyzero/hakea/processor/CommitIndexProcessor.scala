package com.divisiblebyzero.hakea.processor

import java.io.ByteArrayOutputStream

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.Configuration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.solr.DispatchInputDocument
import com.divisiblebyzero.hakea.util.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.diff._
import org.eclipse.jgit.lib.{ Ref, Repository }
import org.eclipse.jgit.revwalk.{ RevCommit, RevWalk }

import scala.collection.JavaConversions._

sealed trait CommitIndexProcessorRequest

case class IndexCommitsFor(project: Project, repository: Repository, refs: List[Ref]) extends CommitIndexProcessorRequest

case class IndexCommit(project: Project, repository: Repository, commit: RevCommit, walk: RevWalk) extends CommitIndexProcessorRequest

case class CommitIndexingComplete(project: Project, repository: Repository, refs: List[Ref]) extends CommitIndexProcessorRequest

class CommitIndexProcessor(configuration: Configuration) extends Actor with Logging {
  protected val commitIndexer = context.actorOf(Props(new CommitIndexer(configuration)), "commitIndexer")

  def receive = {
    case IndexCommitsFor(project, repository, refs) => {
      val walk = new RevWalk(repository)

      walk.markStart(refs.map(ref => walk.parseCommit(ref.getObjectId)))

      log.info("Indexing commit history of %s for refs: %s".format(project.name, refs.map(_.getName)))

      walk.foreach { commit =>
        commitIndexer ! IndexCommit(project, repository, commit, walk)
      }

      commitIndexer ! CommitIndexingComplete(project, repository, refs)
    }
  }
}

class CommitIndexer(configuration: Configuration) extends Actor with Logging {
  protected val inputDocumentDispatcher =
    context.actorFor("/user/hakeaProcessor/repositoryProcessor/indexProcessor/inputDocumentDispatcher")

  protected val indexProcessor = context.actorFor("/user/hakeaProcessor/repositoryProcessor/indexProcessor")

  def receive = {
    case IndexCommit(project, repository, commit, walk) => {
      inputDocumentDispatcher ! DispatchInputDocument(commitToInputDocument(project, repository, commit, walk))
    }
    case CommitIndexingComplete(project, repository, refs) => {
      indexProcessor ! FinishedIndexingCommitsFor(project, repository, refs)
    }
  }

  protected def commitToInputDocument(project: Project, repository: Repository, commit: RevCommit, walk: RevWalk) = {
    val inputDocument = new SolrInputDocument

    inputDocument.addField("id", "commit::%s::%s".format(project.name, commit.getId.name))

    inputDocument.addField("project", project.name)

    inputDocument.addField("commit_author_name", commit.getAuthorIdent.getName)
    inputDocument.addField("commit_author_email", commit.getAuthorIdent.getEmailAddress)
    inputDocument.addField("commit_authored_at", commit.getAuthorIdent.getWhen)

    inputDocument.addField("commit_committer_name", commit.getCommitterIdent.getName)
    inputDocument.addField("commit_committer_email", commit.getCommitterIdent.getEmailAddress)
    inputDocument.addField("commit_committed_at", commit.getCommitterIdent.getWhen)

    inputDocument.addField("commit_subject", commit.getShortMessage)
    inputDocument.addField("commit_message", commit.getFullMessage)

    commit.getFooterLines.filterNot(_.getValue.isEmpty).foreach { footerLine =>
      inputDocument.addField("commit_footer_line_%s_en".format(footerLine.getKey.toLowerCase.replace("-", "_")), footerLine.getValue)
    }

    parseDiffs(repository, commit, walk, inputDocument)

    inputDocument
  }

  /*
   * TODO: Index diffs line-by-line, noting additions and deletions.
   *
   * There is a lot of work that can be done here. The diff format (broken down into files and chunks) lends itself well
   * to indexing. It would be great if Solr could not only index diffs line-by-line (noting additions and deletions),
   * but also files and chunks the changes occurred in.
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

      val diffOutputStreamSize = diffOutputStream.size / 1048576

      // TODO: Index large diffs without bringing down the entire indexer.
      if (diffOutputStreamSize > 100) {
        log.info("Diff for commit [%s] is %sMB, too large for indexing.".format(commit.getId.getName, diffOutputStreamSize))
      } else {
        inputDocument.addField("commit_diff", new String(diffOutputStream.toByteArray))
      }
    }
  }
}
