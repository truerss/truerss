import sbt._

object Libs {
  object Versions {
    val scalaVersion = "2.12.4"
    val scalajVersion = "2.4.1"
    val postgresqlVersion = "42.2.14"
    val mysqlVersion = "8.0.20"
    val sqliteVersion = "3.32.3"
    val commonValidatorVersion = "1.6"
    val truerssBaseVersion = "0.0.3"
    val akkaVersion  = "2.6.3"
    val slickVersion = "3.3.2"
    val configVersion = "1.3.0"
    val scoptVersion = "3.7.0"
    val hikariCPVersion = "3.4.5"
    val jwsVersion = "1.5.1"
    val logbackVersion = "1.1.2"
    val baseVersion = "0.0.6"
    val jsoupVersion = "1.8.3"
    val akkaHttpVersion = "10.1.12"
    val specsVersion = "4.10.0"
    val playJsonVersion = "2.9.0"
    val zioVersion = "1.0.0-RC21-2"
  }

  import Versions._

  val db = Seq(
    "org.postgresql" % "postgresql" % postgresqlVersion,
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
    "com.github.truerss" % "content-extractor" % truerssBaseVersion,
    "com.github.truerss" %% "base" % baseVersion
  )

  val logs = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )

  val jsoup = "org.jsoup" % "jsoup" % jsoupVersion

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion exclude("com.typesafe.akka", "akka-protobuf-v3"),
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
    "org.java-websocket" % "Java-WebSocket" % jwsVersion,
    "dev.zio" %% "zio" % "1.0.0-RC21-2"
  )

  val zio = Seq(
    "dev.zio" %% "zio" % zioVersion
  )

  val tests = Seq(
    "org.specs2" %% "specs2-core" % specsVersion % Test,
    "org.specs2" %% "specs2-mock" % specsVersion % Test,

    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

    "org.testcontainers" % "testcontainers" % "1.14.3" % Test,
    "org.testcontainers" % "mysql" % "1.14.3" % Test,
    "org.testcontainers" % "postgresql" % "1.14.3" % Test,
    "org.testcontainers" % "mockserver" % "1.14.3" % Test
  )

  val deps = db ++ akka ++ truerss ++ logs ++
    Seq(jsoup, playJson) ++ utils ++ zio ++ tests

}
