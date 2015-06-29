package burnup

import github.IssueEvent
import play.api.libs.json.JsValue

import scala.collection.{Set, mutable}

/**
 * Taking a stream of GitHub issue events from a repository, and generate the number of
 * issues in milestones and closed issues over time. The issue events are expected to be
 * in order of the creation.
 *
 * @param oldToNewMilestoneTitles if a milestone was renamed, specify old name -> new name
 */
class IssueEventProcessor (val oldToNewMilestoneTitles: Map[String, String] = Map()) {
  val issueNumberToMilestoneTitle = mutable.Map[Long, String]()
  private val closedIssueNumbers = mutable.Set[Long]()

  val milestoneHistory = mutable.Map[String, MilestoneHistory]()

  /**
   * Update the status of a milestone according to the given issue event
   * @param e new event
   */
  def process(e: IssueEvent): Unit = {
    // Add a data point on both # of milestoned issues and # of closed issues
    def recordDataPoint(milestoneTitle: String): Unit = {
      recordMilestonedIssueDataPoint(e, milestoneTitle)
      recordClosedIssueDataPoint(e, milestoneTitle)
    }

    e.event match {
      // when an issue is milestoned or demilestoned, add a data point
      case "milestoned" =>
        e.milestoneTitle.map { milestoneTitle =>
          val title = oldToNewMilestoneTitles.getOrElse(milestoneTitle, milestoneTitle)
          issueNumberToMilestoneTitle.put(e.issue.number, title)
          recordDataPoint(title)
        }
      case "demilestoned" =>
        // demilestone an issue only if the title in the event is same as the milestone
        // the issue is in. When changing the milestone of an issue from one to the other
        // on GUI, it sometime results in a sequence of "milestone" and "demilestone" events,
        // i.e, reverse order.
        e.milestoneTitle.map { milestoneTitle =>
          val title = oldToNewMilestoneTitles.getOrElse(milestoneTitle, milestoneTitle)
          if (issueNumberToMilestoneTitle.get(e.issue.number).exists(_ == title)) {
            issueNumberToMilestoneTitle.remove(e.issue.number)
            recordDataPoint(title)
          }
        }

      // when an issue is closed or reopened and if the issue is in milestone, add a data point
      case "closed" =>
        closedIssueNumbers.add(e.issue.number)
        issueNumberToMilestoneTitle.get(e.issue.number).map { milestoneTitle =>
          val title = oldToNewMilestoneTitles.getOrElse(milestoneTitle, milestoneTitle)
          recordDataPoint(title)
        }
      case "reopened" =>
        closedIssueNumbers.remove(e.issue.number)
        issueNumberToMilestoneTitle.get(e.issue.number).map { milestoneTitle =>
          val title = oldToNewMilestoneTitles.getOrElse(milestoneTitle, milestoneTitle)
          recordDataPoint(title)
        }

      case _ =>
    }
  }

  /**
   * Record the current list of milestoned issues at the timestamp of the event.
   * If a record already exists at the timestamp, overwrite it.
   */
  private def recordMilestonedIssueDataPoint(e: IssueEvent, milestoneTitle: String): Unit = {
    milestoneHistoryOf(milestoneTitle)
      .milestonedIssueNumbers
      .put(e.timestamp, issueNumbersInMilestone(milestoneTitle))
  }

  /**
   * Record the current list of closed issues at the timestamp of the event.
   * If a record already exists at the timestamp, overwrite it.
   */
  private def recordClosedIssueDataPoint(e: IssueEvent, milestoneTitle: String): Unit = {
    val closedIssuesInMilestone = issueNumbersInMilestone(milestoneTitle) & closedIssueNumbers
    milestoneHistoryOf(milestoneTitle)
      .closedIssueNumbers
      .put(e.timestamp, closedIssuesInMilestone)
  }

  private def milestoneHistoryOf(milestoneTitle: String) = milestoneHistory.getOrElseUpdate(milestoneTitle, new MilestoneHistory())

  /**
   * @return set of issue numbers in the given milestone
   */
  private def issueNumbersInMilestone(milestoneTitle: String): Set[Long] = {
    // can be optimized by retaining (milestone title -> issue numbers). prefer simplicity now.
    issueNumberToMilestoneTitle.filter(_._2 == milestoneTitle).map(_._1).toSet
  }

}
