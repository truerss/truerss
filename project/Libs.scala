import sbt._

object Libs {
  object versions {
    val scalaVersion = "2.11.7"
    val scalajVersion = "2.2.0"
    val rountingExtVersion = "0.3.3"
    val h2Version = "1.3.173"
    val postgresqlVersion = "9.1-901-1.jdbc4"
    val mysqlVersion = "5.1.36"
    val sqliteVersion = "3.8.7"
    val commonValidatorVersion = "1.4.0"
    val ceVersion = "0.0.3"
    val shapelessVersion = "2.1.0"
    val sprayVersion = "1.3.3"
    val sprayJsonVersion = "1.3.2"
    val akkaVersion  = "2.4.14"
    val slickVersion = "3.2.0-M2"
    val configVersion = "1.3.0"
    val scoptVersion = "3.3.0"
    val hikariCPVersion = "2.4.7"
    val jwsVersion = "1.3.0"
    val logbackVersion = "1.1.2"
    val log4j2Version = "1.0.0"
    val baseVersion = "0.0.5"
    val jsoupVersion = "1.8.3"
    val scalaTestVersion = "3.0.0-M7"
    val akkaHttpVersion = "10.0.0"
  }

  import versions._

  val db = Seq(
    "com.h2database" % "h2" % h2Version,
    "postgresql" % "postgresql" % postgresqlVersion,
    "mysql" % "mysql-connector-java" % mysqlVersion,
    "org.xerial" % "sqlite-jdbc" % sqliteVersion,
    "com.zaxxer" % "HikariCP" % hikariCPVersion,
    "com.typesafe.slick" %% "slick" %  slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" %  slickVersion
  )

  val sprayJson = "io.spray" %% "spray-json" % sprayJsonVersion

  val scalaLib = "org.scala-lang" % "scala-library" % scalaVersion

  val truerss = Seq(
    "com.github.truerss" % "content-extractor" % ceVersion,
    "com.github.truerss" %% "base" % baseVersion
  )

  val logs = Seq(
    "org.apache.logging.log4j" % "log4j-api" % "2.5",
    "org.apache.logging.log4j" % "log4j-core" % "2.5",
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "cn.q-game" % "akka-log4j2-logger_2.11" % log4j2Version
  )

  val jsoup = "org.jsoup" % "jsoup" % jsoupVersion

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
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
    "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    "org.specs2" %% "specs2-mock" % "3.8.6" % "test",

    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
  )

  val deps = db ++ akka ++ truerss ++ logs ++
    Seq(jsoup, sprayJson) ++ utils ++ tests

}
