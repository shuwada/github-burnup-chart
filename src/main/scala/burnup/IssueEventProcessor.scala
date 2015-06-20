package burnup

import github.IssueEvent

import scala.collection.{Set, mutable}

/**
 * Taking a stream of GitHub issue events from a repository, and generate the number of
 * issues in milestones and closed issues over time. The issue events are expected to be
 * in order of the creation.
 */
class IssueEventProcessor {
  private val issueNumberToMilestoneTitle = mutable.Map[Long, String]()
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
          issueNumberToMilestoneTitle.put(e.issueNumber, milestoneTitle)
          recordDataPoint(milestoneTitle)
        }
      case "demilestoned" =>
        issueNumberToMilestoneTitle.remove(e.issueNumber).map { milestoneTitle =>
          recordDataPoint(milestoneTitle)
        }

      // when an issue is closed or reopened and if the issue is in milestone, add a data point
      case "closed" =>
        closedIssueNumbers.add(e.issueNumber)
        issueNumberToMilestoneTitle.get(e.issueNumber).map { milestoneTitle =>
          recordDataPoint(milestoneTitle)
        }
      case "reopened" =>
        closedIssueNumbers.remove(e.issueNumber)
        issueNumberToMilestoneTitle.get(e.issueNumber).map { milestoneTitle =>
          recordDataPoint(milestoneTitle)
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
