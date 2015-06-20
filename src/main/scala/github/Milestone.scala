package github

import java.time.ZonedDateTime

/**
 * Subset of milestone. See https://developer.github.com/v3/issues/milestones/
 */
case class Milestone (
  id: Long,
  title: String,
  due: Option[ZonedDateTime],
  closedAt: Option[ZonedDateTime],
  url: String
)
