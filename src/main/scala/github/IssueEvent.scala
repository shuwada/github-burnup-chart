package github

import java.time.ZonedDateTime

/**
 * Subset of issue event. See https://developer.github.com/v3/issues/events/
 */
case class IssueEvent (
  id: Long,
  timestamp: ZonedDateTime,
  event: String,
  issueNumber: Long,
  milestoneTitle: Option[String],
  isPullRequest: Boolean
)
