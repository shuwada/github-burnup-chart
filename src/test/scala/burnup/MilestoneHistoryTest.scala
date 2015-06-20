package burnup

import java.time.ZonedDateTime
import github.Milestone
import org.specs2.mutable._

class MilestoneHistoryTest extends Specification {

  "First event timestamps" should {
    "be None if MilestoneHistory has no event nor due" in {
      val h = new MilestoneHistory()
      h.from must beNone
    }

    "be the first event" in {
      val h = testMilestoneHistory
      h.from must beSome(ZonedDateTime.parse("2015-06-09T00:00:00Z"))
    }

    "be the milestone due if it's before the first event" in {
      val h = testMilestoneHistory
      h.milestone = Some(Milestone(1, "title", Some(ZonedDateTime.parse("2015-06-01T01:00:00Z")), Option.empty, "url"))
      h.from must beSome(ZonedDateTime.parse("2015-06-01T01:00:00Z"))
    }
  }

  "Last event timestamps" should {
    "be None if MilestoneHistory has no event nor due" in {
      val h = new MilestoneHistory()
      h.to must beNone
    }

    "be the last event" in {
      val h = testMilestoneHistory
      h.to must beSome(ZonedDateTime.parse("2015-06-11T01:00:00Z"))
    }

    "be the last event if it's after the due" in {
      val h = testMilestoneHistory
      h.milestone = Some(Milestone(1, "title", Some(ZonedDateTime.parse("2015-06-10T01:00:00Z")), Option.empty, "url"))
      h.to must beSome(ZonedDateTime.parse("2015-06-11T01:00:00Z"))
    }

    "be milestone due if it's after the last event" in {
      val h = testMilestoneHistory
      h.milestone = Some(Milestone(1, "title", Some(ZonedDateTime.parse("2015-06-15T01:00:00Z")), Option.empty, "url"))
      h.to must beSome(ZonedDateTime.parse("2015-06-15T01:00:00Z"))
    }

  }

  def testMilestoneHistory: MilestoneHistory = {
    val h = new MilestoneHistory()

    // milestone issue events
    Seq("2015-06-10T00:00:00Z", "2015-06-09T00:00:00Z", "2015-06-11T00:00:00Z")
      .map(ZonedDateTime.parse(_))
      .foreach { t =>
        h.milestonedIssueNumbers.put(t, Set(1000L))
      }

    // close issue events
    Seq("2015-06-09T01:00:00Z", "2015-06-11T01:00:00Z", "2015-06-10T01:00:00Z")
      .map(ZonedDateTime.parse(_))
      .foreach { t =>
        h.milestonedIssueNumbers.put(t, Set(1000L))
      }

    h
  }

}
