GitHub Milestone Burnup Chart Genrator
======================================

What is this?
--------------------------------------

A CLI tool to generate a burnup chart from each milestone in your GitHub repository.

A picture is worth a thousand words. This is an example chart you'll get.

![Burnup Chart](/examples/example.png?raw=true)

The actual output is in HTML. Example output files can be found in `/examples` ([this](http://htmlpreview.github.io/?https://raw.githubusercontent.com/shuwada/github-burnup-chart/master/examples/completed.html) and [this](http://htmlpreview.github.io/?https://github.com/shuwada/github-burnup-chart/blob/master/examples/inprogress.html).)

Burn**down** chart is a great tool to track the progress of your sprints. The problem is, it's easy to make a burndown chart look pretty by kicking outstanding issues out of a sprint. Your manager is probably happy with it but it's less useful for your team's sprint retrospective - that's exactly the reason I wrote this tool.


Usage
--------------------------------------

Install Java 8 SDK. The rest will be taken care of by [Typesafe Activator](https://www.typesafe.com/community/core-tools/activator-and-sbt), which is included in this repo.

Execute the following command in the top directory of this repo. Make sure to put all parameters in double (or single) quotes.

```
./activator "run -u username -p password -o owner -r repo -d ./charts"
```
or
```
activator.bat "run -u username -p password -o owner -r repo -d .\charts"
```

It fetches data from `https://github.com/owner/repo` and generates burnup charts in `./charts`.


#### Options

```
  -u <value> | --username <value>
        GitHub username. For authenticating with username/password.
  -p <value> | --password <value>
        GitHub password. For authenticating with username/password.
  -t <value> | --token <value>
        GitHub access token. For authenticating with OAuth.
  -o <value> | --owner <value>
        Owner of the GitHub repository to scan.
  -r <value> | --repo <value>
        Name of the GitHub repository to scan.
  -s <value> | --since <value>
        Process GitHub issue events since this date in YYYY-MM-DD, e.g., 2015-06-03.
  -d <value> | --outdir <value>
        Directory to write output
  -t <value> | --timezone <value>
        Optional timezone. Use the system timezone if not given.
```


Limitations
--------------------------------------

- GitHub does not keep track of changes made in milestones. For example, if a milestone had been renamed, two charts will be generated for each, i.e., one for the period till the rename happened and another for the period after the rename.

- Due to the same reason above only the current milestone due date is displayed on a chart. Also, if a milestone had been removed from the repository, the milestone due is not available via API and the chart shows no due date.

- The tool fetches GitHub issue events every time, i.e., no cache is implemented. It could be a problem if your repository has a large number of issues and activities. But just FYI - it takes only 4 or 5 minutes to fetch all data and draw charts from a repository with ~5,000 issues, ~12,000 commits and 50 milestones. 

- API limit (https://developer.github.com/v3/#rate-limiting) could also be a problem when a repository is huge. It requires about ~300 API calls to fetch all data from the example repository above.

- `--since` option may not work as you expect. When a milestone was created before the nominated date, the chart may not show data properly since all activities before the date are ignored on event processing.
