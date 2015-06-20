name := """github-burnup"""

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.4.1" exclude("commons-logging", "commons-logging")
libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.4.0"

// Logging
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.13"
libraryDependencies += "org.slf4j" % "jcl-over-slf4j" % "1.7.12"

// CLI
resolvers += Resolver.sonatypeRepo("public")
libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

// Test
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
libraryDependencies += "org.specs2" % "specs2-core_2.11" % "3.6.1"
