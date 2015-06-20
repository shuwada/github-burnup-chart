package burnup

import java.time.{LocalDate, ZonedDateTime, DayOfWeek}

import org.specs2.mutable._

class UtilSpec extends Specification {

  "the response of dayOfWeekBetween" should {
    "include the first day if it's the nominated day" in {
      val from = ZonedDateTime.parse("2015-06-15T00:00:00Z") // Monday
      val to = ZonedDateTime.parse("2015-07-14T00:00:00Z") // Tuesday
      val tuesdays = Util.dayOfWeekBetween(DayOfWeek.MONDAY, from, to)

      val expected = Seq("2015-06-15", "2015-06-22", "2015-06-29", "2015-07-06", "2015-07-13").map(LocalDate.parse(_))
      tuesdays must containTheSameElementsAs(expected)
    }

    "include the last day if it's the nominated day" in {
      val from = ZonedDateTime.parse("2015-06-15T00:00:00Z") // Monday
      val to = ZonedDateTime.parse("2015-07-14T00:00:00Z") // Tuesday
      val tuesdays = Util.dayOfWeekBetween(DayOfWeek.TUESDAY, from, to)

      val expected = Seq("2015-06-16", "2015-06-23", "2015-06-30", "2015-07-07", "2015-07-14").map(LocalDate.parse(_))
      tuesdays must containTheSameElementsAs(expected)
    }

    "find nothing when the nominated day is not included" in {
      val from = ZonedDateTime.parse("2015-06-01T00:00:00Z")
      val to = ZonedDateTime.parse("2015-06-05T00:00:00Z")
      Util.dayOfWeekBetween(DayOfWeek.SUNDAY, from, to) must beEmpty
    }
  }

  "Filename escape" should {
    "replace invalid characters with _" in {
      Util.escape("""< > : " / \ | ? *""", true) must be equalTo("_ _ _ _ _ _ _ _ _")
    }
    "not replace valid characters" in {
      Util.escape("""abc123~!@#$%^&.()-_+=""", true) must be equalTo("abc123~!@#$%^&.()-_+=")
    }
  }

}
