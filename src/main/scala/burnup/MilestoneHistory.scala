package burnup

import java.time.{LocalDate, DayOfWeek, ZonedDateTime}
import java.util.TimeZone

import github.Milestone

import scala.collection.JavaConversions._
import scala.collection.{Set, SortedSet}
import scala.collection.immutable.TreeSet

/**
 * # of open and closed issues over time in a milestone
 */
class MilestoneHistory {
  val milestonedIssueNumbers = new java.util.TreeMap[ZonedDateTime, Set[Long]]()
  val closedIssueNumbers = new java.util.TreeMap[ZonedDateTime, Set[Long]]()

  var milestone: Option[Milestone] = None

  /**
   * @return the data point before but the closest to the given timestamp
   */
  def milestonedIssueNumbersAt(t: ZonedDateTime): Set[Long] = {
    Option(milestonedIssueNumbers.navigableKeySet().floor(t)) map { key =>
      milestonedIssueNumbers(key)
    } getOrElse(Set())
  }

  /**
   * @return the data point before but the closest to the given timestamp
   */
  def closedIssuesNumbersAt(t: ZonedDateTime): Set[Long] = {
    Option(closedIssueNumbers.navigableKeySet().floor(t)) map { key =>
      closedIssueNumbers(key)
    } getOrElse(Set())
  }

  def from: Option[ZonedDateTime] = {
    eventTimestamps.headOption
  }

  def to: Option[ZonedDateTime] = {
    eventTimestamps.lastOption
  }

  def eventTimestamps: SortedSet[ZonedDateTime] = {
    val timestamps = milestonedIssueNumbers.keySet() ++ closedIssueNumbers.keySet() ++ milestone.flatMap(_.due)
    SortedSet()(Ordering[Long].on[ZonedDateTime](_.toEpochSecond)) ++ timestamps
  }

}
