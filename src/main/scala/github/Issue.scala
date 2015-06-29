package github

import play.api.libs.json.{JsNull, JsValue}

/**
 * Subset of issue event. See https://developer.github.com/v3/issues/events/
 */
case class Issue (
  number: Long,
  state: String,
  title: String,
  milestone: Option[Milestone],
  isPullRequest: Boolean
) {
  def this (jsValue: JsValue) = this (
    (jsValue \ "number").as[Long],
    (jsValue \ "state").as[String],
    (jsValue \ "title").as[String],
    (jsValue \ "milestone").as[JsValue] match {
      case v if v == JsNull => None
      case v:JsValue => Some(new Milestone(v))
    },
    (jsValue \ "pull_request").toOption.isDefined
  )
}
