package cli

import java.io.File
import java.time.{LocalDate, ZonedDateTime}
import java.util.TimeZone

case class CliArgs(
  username: Option[String] = Option.empty,
  password: Option[String] = Option.empty,
  accessToken: Option[String] = Option.empty,
  owner: String = "",
  repo: String = "",
  since: Option[LocalDate] = Option.empty,
  outputDir: File = new File("."),
  timeZone: TimeZone = TimeZone.getDefault
)

object CliUtil {

  val parser = new scopt.OptionParser[CliArgs]("run") {
    head("GitHub github.Milestone Burnup Chart Generator")

    opt[String]('u', "username") action { (x, c) => c.copy(username = Option(x)) } text("GitHub username. For authenticating with username/password.")
    opt[String]('p', "password") action { (x, c) => c.copy(password = Option(x)) } text("GitHub password. For authenticating with username/password.")
    opt[String]('t', "token") action { (x, c) => c.copy(accessToken = Option(x)) } text("GitHub access token. For authenticating with OAuth.")
    opt[String]('o', "owner") required() action { (x, c) => c.copy(owner = x) } text("Owner of the GitHub repository to scan.")
    opt[String]('r', "repo") required() action { (x, c) => c.copy(repo = x) } text("Name of the GitHub repository to scan.")

    opt[String]('s', "since") action { (x, c) =>
      c.copy(since = Option(LocalDate.parse(x)))
    } text("Process GitHub issue events since this date in YYYY-MM-DD, e.g., 2015-06-03.")

    opt[File]('d', "outdir") required() action { (x, c) =>
      c.copy(outputDir = x)
    } validate { x =>
      if (x.isDirectory && x.canWrite) success else failure(s"$x is not a directory or not writable.")
    } text("Directory to write output")

    opt[String]('t', "timezone") action { (x, c) =>
      c.copy(timeZone = TimeZone.getTimeZone(x))
    } validate { x =>
      if (TimeZone.getAvailableIDs.contains(x)) success else failure(s"$x is not valid TimeZone.")
    } text("Optional timezone. Use the system timezone if not given.")

    checkConfig { args =>
      val isPassword = args.username.isDefined && args.password.isDefined
      val isToken = args.accessToken.isDefined

      if ((isPassword && isToken) || (!isPassword && !isToken))
        failure("Either password-based and token-based authentication is required.")
      else
        success
    }
  }

}