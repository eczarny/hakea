package com.divisiblebyzero.hakea.indexing.processor

import java.io.ByteArrayOutputStream

import akka.actor.{ Actor, Props }

import com.divisiblebyzero.hakea.config.HakeaConfiguration
import com.divisiblebyzero.hakea.model.Project
import com.divisiblebyzero.hakea.indexing.solr.{ DispatchInputDocument, InputDocumentDispatcher }
import com.yammer.dropwizard.Logging
import org.apache.solr.common.SolrInputDocument
import org.eclipse.jgit.diff._
import org.eclipse.jgit.lib.{ Ref, Repository }
import org.eclipse.jgit.revwalk.{ RevCommit, RevWalk }

import scala.collection.JavaConversions._

sealed trait CommitIndexProcessorRequest

case class IndexCommitsFor(project: Project, repository: Repository, refs: List[Ref]) extends CommitIndexProcessorRequest

case class IndexCommit(project: Project, repository: Repository, commit: RevCommit, walk: RevWalk) extends CommitIndexProcessorRequest

case class CommitIndexingComplete(project: Project, repository: Repository, refs: List[Ref]) extends CommitIndexProcessorRequest

class CommitIndexProcessor(configuration: HakeaConfiguration) extends Actor with Logging {
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

class CommitIndexer(configuration: HakeaConfiguration) extends Actor with Logging {
  protected val inputDocumentDispatcher =
    context.actorFor("/user/projectProcessor/repositoryProcessor/indexProcessor/inputDocumentDispatcher")

  protected val indexProcessor = context.actorFor("/user/projectProcessor/repositoryProcessor/indexProcessor")

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

    inputDocument.addField("id", "commit::%s".format(commit.getId.name))

    // TODO: Find a way of indexing the refs a commit belongs to.
//    inputDocument.addField("refs", ref.getName)
    inputDocument.addField("project", project.name)

    inputDocument.addField("commit_author_name", commit.getAuthorIdent.getName)
    inputDocument.addField("commit_author_email", commit.getAuthorIdent.getEmailAddress)
    inputDocument.addField("commit_author_date", commit.getAuthorIdent.getWhen)
    inputDocument.addField("commit_committer_name", commit.getCommitterIdent.getName)
    inputDocument.addField("commit_committer_email", commit.getCommitterIdent.getEmailAddress)
    inputDocument.addField("commit_committer_date", commit.getCommitterIdent.getWhen)
    inputDocument.addField("commit_full_message", commit.getFullMessage)
    inputDocument.addField("commit_short_message", commit.getShortMessage)

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
      if (diffOutputStreamSize > 10) {
        log.info("Diff for commit [%s] is %sMB, too large for indexing.".format(commit.getId.getName, diffOutputStreamSize))
      } else {
        inputDocument.addField("commit_diff", new String(diffOutputStream.toByteArray))
      }
    }
  }
}
