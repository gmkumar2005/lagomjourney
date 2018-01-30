lazy val root = (project in file("."))
  .settings(name := "lagomJourney")
  .aggregate(CMSOrdersApi,  CMSOrdersImpl)
  .settings(commonSettings: _*)

organization in ThisBuild := "CMSOrders"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "4.0.0"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"
val scalaMoney = "com.github.nscala-money" %% "nscala-money" % "0.13.0"
val scalaMoneyPlay = "com.github.nscala-money" %% "nscala-money-play-json" % "0.13.0"
val playJoda = "com.typesafe.play" %% "play-json-joda" % "2.6.8"
val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.18.0"

def commonSettings: Seq[Setting[_]] = Seq(

)

lagomCassandraCleanOnStart in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false


lazy val CMSOrdersApi = (project in file("CMSOrders-api"))
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      scalaMoney,
      scalaMoneyPlay,
      playJoda,
      nscalaTime
      //      playJsonDerivedCodecs,

    )
  )


lazy val  CMSOrdersImpl = (project in file("CMSOrders-impl"))
  .settings(commonSettings: _*)
  .enablePlugins(LagomScala)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      playJoda,
      nscalaTime,
      scalaMoney,
      scalaMoneyPlay,
      //      "com.datastax.cassandra" % "cassandra-driver-extras" % "3.0.0",

      macwire,
      scalaTest
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(CMSOrdersApi)


// ------------------------------------------------------------------------------------------------

// register 'elastic-search' as an unmanaged service on the service locator so that at 'runAll' our code
// will resolve 'elastic-search' and use it. See also com.example.com.ElasticSearch
lagomUnmanagedServices in ThisBuild += ("elastic-search" -> "http://127.0.0.1:9200")

