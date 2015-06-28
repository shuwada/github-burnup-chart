package github

import java.time.ZonedDateTime

import play.api.libs.json.JsValue

/**
 * Subset of milestone. See https://developer.github.com/v3/issues/milestones/
 */
case class Milestone (
  id: Long,
  title: String,
  due: Option[ZonedDateTime],
  closedAt: Option[ZonedDateTime],
  url: String
) {
  def this(jsValue: JsValue) = this(
    (jsValue \ "id").as[Long],
    (jsValue \ "title").as[String],
    (jsValue \ "due_on").asOpt[String].map(ZonedDateTime.parse),
    (jsValue \ "closed_at").asOpt[String].map(ZonedDateTime.parse),
    (jsValue \ "url").as[String]
  )
}
