package burnup

import java.time.format.DateTimeFormatter
import java.time._
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Template to generate the output in HTML
 */
object OutputTemplate {

  /**
   * @param title Title of the chart
   * @param issues # of issues over time
   * @param closedIssues # of closed issues over time
   * @param saturdays list of Saturday in LocalDate
   * @param dueDateTime Due date. Optional.
   * @param timeZone Timezone
   * @return output in HTML
   */
  def chart(title: String,
            issues: Map[ZonedDateTime, Int],
            closedIssues: Map[ZonedDateTime, Int],
            dueDateTime: Option[ZonedDateTime],
            saturdays: Seq[LocalDate],
            min: Option[ZonedDateTime],
            max: Option[ZonedDateTime],
            timeZone: TimeZone) = s"""
<html>
<head>
  <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js"></script>
  <script src="http://code.highcharts.com/highcharts.js"></script>
  <script>

$$(function () {
  Highcharts.setOptions({
    global: {
      timezoneOffset: ${-1 * TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset)}
    }
  });

  $$('#container').highcharts({
    chart: {
      type: 'area'
    },

    title: {
      text: '$title'
    },

    xAxis: {
    ${min.map { m => s"""min: ${toMilliseconds(m)},""" } getOrElse("")}
    ${max.map { m => s"""max: ${toMilliseconds(m)},""" } getOrElse("")}
    ${dueDateTime.map { due =>
      val localDue = due.withZoneSameInstant(timeZone.toZoneId)
      s"""
      plotLines: [{
        color: 'red',
        value: ${toMilliseconds(localDue)},
        width: 2,
        label: {
          "text": "Due (${localDue.format(DateTimeFormatter.ofPattern("MMMM dd"))})",
          "verticalAlign": "middle"
        },
        zIndex: 999
      }],
      """
    }.getOrElse("")}
    ${saturdays.map { saturday =>
      // Get Saturday 00:00 in the nominated TimeZone, then, convert it in UTC
      val zonedSaturdayInUTC = ZonedDateTime.of(saturday, LocalTime.of(0, 0), timeZone.toZoneId)
      s"""{
        from: ${toMilliseconds(zonedSaturdayInUTC)},
        to: ${toMilliseconds(zonedSaturdayInUTC.plusDays(2))},
        color: '#CCFFDD'
      }"""
    }.mkString("plotBands: [", ",", "],")}
      type: 'datetime',
      dateTimeLabelFormats: {
          day: '%b %e',
          week: '%b %e'
      }
    },

    yAxis: {
      title: {
        text: 'Number of Issues'
      },
      min: 0
    },

    tooltip: {
      headerFormat: '<b>{series.name}</b><br>',
      pointFormat: '{point.y:f} on {point.x:%b %e %H:%M}'
    },

    plotOptions: {
      spline: {
        marker: {
          enabled: true
        }
      }
    },

    legend: {
      spline: {
        marker: {
          enabled: true
        }
      }
    },

    series: [{
      name: 'Issues in Milestone',
      color: '#FE9A2E',
      data: ${toJS(issues)}
    }, {
      name: 'Closed Issues',
      color: '#0066FF',
      data: ${toJS(closedIssues)}
    }]

  });
});

    </script>
  </head>
  <body>
    <div id="container" style="width: 600px; height: 400px; margin: 0 auto"></div>
  </body>
</html>
""".trim


  /**
   * @return convert ZonedDateTime to Date in JavaScript
   */
  private def toMilliseconds(t: ZonedDateTime): String = s"${t.toEpochSecond * 1000}"

  private def toJS(issues: Map[ZonedDateTime, Int]): String = {
    issues.toSeq
      .sortBy(_._1.toEpochSecond)
      .map(e => s"[${toMilliseconds(e._1)}, ${e._2}]")
      .mkString("[", ",", "]")
  }

}