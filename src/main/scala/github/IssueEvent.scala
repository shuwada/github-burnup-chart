package github

import java.time.ZonedDateTime

import play.api.libs.json.JsValue

/**
 * Subset of issue event. See https://developer.github.com/v3/issues/events/
 */
case class IssueEvent (
  id: Long,
  timestamp: ZonedDateTime,
  event: String,
  issue: Issue,
  milestoneTitle: Option[String],
  isPullRequest: Boolean
) {
  def this (jsValue: JsValue) = this (
    (jsValue \ "id").as[Long],
    ZonedDateTime.parse((jsValue \ "created_at").as[String]),
    (jsValue \ "event").as[String],
    new Issue((jsValue \ "issue").as[JsValue]),
    (jsValue \ "milestone" \ "title").asOpt[String],
    (jsValue \ "pull_request").toOption.isDefined
  )
}
