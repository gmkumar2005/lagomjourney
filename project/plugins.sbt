//
// Copyright (C) 2016 Lightbend Inc. <https://www.lightbend.com>
//

// The Lagom plugin
addSbtPlugin("com.lightbend.lagom" % "lagom-sbt-plugin" % "1.4.0-RC1")
// Needed for importing the project into Eclipse
//addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.1.0")
// The ConductR plugin
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr" % "2.5.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.11")