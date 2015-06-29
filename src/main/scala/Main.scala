import java.io.{BufferedInputStream, File}
import java.nio.file.{Files, Path, Paths}
import java.time._
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

import burnup._
import cli.CliUtil._
import github.{Issue, IssueEvent, Milestone, GitHubClient}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.collection.JavaConversions._
import scala.collection.{mutable, Set}


object Main extends App {
  val logger = LoggerFactory.getLogger(this.getClass)

  // parse the command line options
  parser.parse(args, cli.CliArgs()) match {
    case Some(config) =>
      logger.info(s"Generate milestone burnup charts of https://github.com/${config.owner}/${config.repo}")
      logger.info(s"Time Zone: ${config.timeZone.getID}")

      // initialize GitHubClient
      val gitHubClient = config
        .accessToken.map(new GitHubClient(_))
        .orElse(for (u <- config.username; p <- config.password) yield new GitHubClient(u, p))
        .get

      val since = config.since.map(date => ZonedDateTime.of(date, LocalTime.of(0, 0), config.timeZone.toZoneId))
      logger.info(s"Fetching issue events${since.map(" since " + _.toString).getOrElse("")} ...")
      val issueEventsPath = fetchGitHubIssueEvents(gitHubClient, config.owner, config.repo, since)
      // val issueEventsPath = Paths.get("/tmp/xxx") // read local cache
      val milestones = fetchGitHubMilestones(gitHubClient, config.owner, config.repo)
      logger.info(s"Finished fetching data from GitHub")

      // analyze GitHub issue events
      val eventProcessor = analyzeIssueEvents(issueEventsPath, milestones)

      // set milestone details
      val milestonesHistory = eventProcessor.milestoneHistory.toMap
      milestonesHistory.foreach { case (title, history) =>
        history.milestone = milestones.find(_.title == title)
      }

      // generate a chart per milestone
      generateBurnupCharts(milestonesHistory, config.timeZone, config.outputDir)

    case None =>
  }

  /**
   * Fetch /repos/:owner/:repo/issue/events and save into a file
   * @return path to the file containing issue events in JSON
   */
  def fetchGitHubIssueEvents(
    githubClient: GitHubClient, owner: String, repo: String,
    till: Option[ZonedDateTime] = Option.empty,
    outFile: Path = Files.createTempFile("github-issues", ".json")
  ): Path = {
    val stream = Files.newBufferedWriter(outFile)
    try {
      val isFirstElement = new AtomicBoolean(true)
      stream.write("[")
      githubClient
        .pagedGet(s"/repos/${owner}/${repo}/issues/events")
        .flatMap(_.value)
        .takeWhile(j => till.map(_.isBefore(ZonedDateTime.parse((j \ "created_at").as[String]))).getOrElse(true))
        .foreach { json =>
          // write ',' if not the first element. note you need to synchronize this block when using parallel collection.
          if (isFirstElement.getAndSet(false) == false)
            stream.write(",")
          stream.write(Json.prettyPrint(json))
        }
      stream.write("]")

      logger.debug(s"Wrote GitHub issue events in ${outFile.toAbsolutePath} temporarily")
      outFile
    } finally {
      stream.close()
    }
  }

  /**
   * Fetch /repos/:owner/:repo/milestones?state=all
   * @return list of milestones
   */
  def fetchGitHubMilestones(githubClient: GitHubClient, owner: String, repo: String): Seq[Milestone] = {
    githubClient
      .pagedGet(s"/repos/$owner/$repo/milestones", Map("state" -> "all"))
      .flatMap(_.value)
      .map(new Milestone(_))
      .toSeq
  }

  /**
   * @param issueEventsPath path of a file containing the response of 'GET /repos/:owner/:repo/issues/events'
   *                        in the order of the creation
   */
  def analyzeIssueEvents(issueEventsPath: Path, milestones: Seq[Milestone]): IssueEventProcessor = {
    logger.info(s"Analyzing GitHub issue events...")
    val stream = new BufferedInputStream(Files.newInputStream(issueEventsPath))
    try {
      analyzeIssueEvents(Json.parse(stream).as[JsArray].value)
    } finally {
      logger.info(s"Finished analyzing GitHub issue events")
      stream.close()
    }
  }

  /**
   * @param issueEvents list of issue events in reverse chronological order
   */
  def analyzeIssueEvents(issueEvents: Seq[JsValue]): IssueEventProcessor = {
    // Reverse the order of issue events
    val events = issueEvents.map(new IssueEvent(_))
      .filter(_.issue.isPullRequest == false)
      .reverse

    // To find if milestones are renamed, check the current milestone of each issue.
    // Each issue event has 'milestone' property and it retains the *current* milestone
    // the issue is in. If the milestone was renamed, the *current* one is also renamed too.
    val issueNumberToCurrentMilestoneTitle = events
      .flatMap(e => e.issue.milestone map (e.issue.number -> _.title))
      .toMap

    // Process the events. The result keeps the name of the *last* milestone each issue
    // was put into. The name is the one when the issue was milestoned - it has nothing
    // to do with the current name if the milestone was renamed.
    logger.debug(s"Processing the issue events.")
    val preProcessor = new IssueEventProcessor
    events.foreach(preProcessor.process(_))

    // If an issue is milestoned now and the current name of the milestone name is different
    // from that of the milestone when the issue was milestoned, the milestone was renamed
    // since the issue was milestoned
    val oldToNewMilestoneTitles = (for (
      key <- issueNumberToCurrentMilestoneTitle.keys ++ preProcessor.issueNumberToMilestoneTitle.keys;
      currentName <- issueNumberToCurrentMilestoneTitle.get(key);
      lastName <- preProcessor.issueNumberToMilestoneTitle.get(key);
      if lastName != currentName
    ) yield (lastName, currentName)) toMap

    // If no milestone was renamed, it's done. Otherwise, do processing again.
    oldToNewMilestoneTitles match {
      case map if map.isEmpty => preProcessor
      case map =>
        logger.debug(s"Milestones renamed: ${oldToNewMilestoneTitles}")

        // Do process all events again. In this path, consider milestones renamed.
        logger.debug(s"Processing the issue events again with renamed milestones considered.")
        val processor = new IssueEventProcessor(oldToNewMilestoneTitles)
        events.foreach(processor.process(_))
        processor
    }
  }

  /**
   * Generate a burnup chart for each milestone
   */
  def generateBurnupCharts(milestonesHistory: Map[String, MilestoneHistory], timeZone: TimeZone, outputDir: File) {

    // add two days margin to x-axis
    val marginDays = 2

    milestonesHistory.map { case (milestoneTitle, milestoneHistory) =>

      // if the milestone is open, set max of x-axis to cover today
      val isOpen = milestoneHistory.milestone.map(_.closedAt.isEmpty).getOrElse(false)
      val max = (milestoneHistory.eventTimestamps ++ (if (isOpen) Set(ZonedDateTime.now) else Nil)).lastOption.map(_.plusDays(marginDays))
      val min = milestoneHistory.from.map(_.minusDays(marginDays))

      // find all weekends in this milestone
      val saturdays = (for (from <- min; to <- max)
        yield Util.dayOfWeekBetween(
          DayOfWeek.SATURDAY,
          from.withZoneSameInstant(timeZone.toZoneId), to.withZoneSameInstant(timeZone.toZoneId)
        )
      ).getOrElse(Nil)

      def numberOfIssuesOverTime(issues: java.util.TreeMap[ZonedDateTime, Set[Long]]): Map[ZonedDateTime, Int] = {
        val dataPoints = issues.map(d => (d._1 -> d._2.size)).toMap
        val lastNumOfIssues = issues.lastOption.map(_._2.size).getOrElse(0)
        // if the milestone is open, add today as as a new data point
        dataPoints ++ (if (isOpen) Map(ZonedDateTime.now() -> lastNumOfIssues) else Nil)
      }

      val output = OutputTemplate.chart(
        milestoneTitle,
        numberOfIssuesOverTime(milestoneHistory.milestonedIssueNumbers),
        numberOfIssuesOverTime(milestoneHistory.closedIssueNumbers),
        milestoneHistory.milestone.flatMap(_.due),
        saturdays,
        min,
        max,
        timeZone
      )

      (milestoneTitle, output)
    }.foreach { case (milestoneTitle, output) =>
      val escapedFilename = Util.escape(milestoneTitle)
      val path = Paths.get(s"$outputDir/$escapedFilename.html")
      Files.write(path, output.getBytes)
      logger.info(s"Generated ${path.toAbsolutePath}")
    }
  }

}


