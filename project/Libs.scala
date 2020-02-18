import sbt._

object Libs {
  object Versions {
    val scalaVersion = "2.12.4"
    val scalajVersion = "2.4.1"
    val h2Version = "1.3.173"
    val postgresqlVersion = "9.1-901-1.jdbc4"
    val mysqlVersion = "5.1.36"
    val sqliteVersion = "3.8.7"
    val commonValidatorVersion = "1.6"
    val ceVersion = "0.0.3"
    val akkaVersion  = "2.6.3"
    val slickVersion = "3.2.3"
    val configVersion = "1.3.0"
    val scoptVersion = "3.7.0"
    val hikariCPVersion = "2.4.7"
    val jwsVersion = "1.3.9"
    val logbackVersion = "1.1.2"
    val baseVersion = "0.0.6"
    val jsoupVersion = "1.8.3"
    val akkaHttpVersion = "10.1.11"
    val specsVersion = "4.8.3"
    val playJsonVersion = "2.8.1"
  }

  import Versions._

  val db = Seq(
    "com.h2database" % "h2" % h2Version,
    "postgresql" % "postgresql" % postgresqlVersion,
    "mysql" % "mysql-connector-java" % mysqlVersion,
    "org.xerial" % "sqlite-jdbc" % sqliteVersion,
    "com.zaxxer" % "HikariCP" % hikariCPVersion,
    "com.typesafe.slick" %% "slick" %  slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" %  slickVersion,
    "io.github.nafg" %% "slick-migration-api" % "0.4.2"
  )

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val scalaLib = "org.scala-lang" % "scala-library" % scalaVersion

  val truerss = Seq(
    "com.github.truerss" % "content-extractor" % ceVersion,
    "com.github.truerss" %% "base" % baseVersion
  )

  val logs = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )

  val jsoup = "org.jsoup" % "jsoup" % jsoupVersion

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  )

  val utils = Seq(
    "org.scalaj" %% "scalaj-http" % scalajVersion,
    "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
   ("commons-validator" % "commons-validator" % commonValidatorVersion)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-logging", "commons-logging")
      .exclude("commons-digester", "commons-digester")
      .exclude("commons-collections", "commons-collections"),
    "com.typesafe" % "config" % configVersion,
    "com.github.scopt" %% "scopt" % scoptVersion,
    "org.java-websocket" % "Java-WebSocket" % jwsVersion
  )

  val tests = Seq(
    "org.specs2" %% "specs2-core" % specsVersion % "test",
    "org.specs2" %% "specs2-mock" % specsVersion % "test",

    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  )

  val deps = db ++ akka ++ truerss ++ logs ++
    Seq(jsoup, playJson) ++ utils ++ tests

}
