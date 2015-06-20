package burnup

import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.time.{Duration, DayOfWeek, LocalDate, ZonedDateTime}

object Util {

  /**
   * @return list of Saturdays (in LocalDate) between from to to. Hours and smaller fields are all sets 0.
   */
  def dayOfWeekBetween(dayOfWeek: DayOfWeek, from: ZonedDateTime, to: ZonedDateTime): Seq[LocalDate] = {
     (0L to Duration.between(from, to).toDays)
      .map(from.toLocalDate.plusDays(_))
      .filter(_.getDayOfWeek == dayOfWeek)
  }

  /**
   * @return Replace invalid file name characters to "_"
   */
  def escape(filename: String, isWindows: Boolean = System.getProperty("os.name").toLowerCase().contains("win")) = {
    isWindows match {
      case true => filename.replaceAll( """[<>:"/\\|?*]""", "_")
      case false => filename.replaceAll("/", "_")
    }
  }

}
