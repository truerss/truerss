import sbt._

object Libs {
  object Versions {
    val scalaVersion = "2.13.6"
    val scalajVersion = "2.4.2"
    val postgresqlVersion = "42.2.14"
    val mysqlVersion = "8.0.21"
    val sqliteVersion = "3.32.3"
    val commonValidatorVersion = "1.6"
    val contentExtractorVersion = "1.0.5"
    val slickVersion = "3.3.2"
    val configVersion = "1.3.0"
    val scoptVersion = "4.0.1"
    val hikariCPVersion = "3.4.5"
    val jwsVersion = "1.5.1"
    val logbackVersion = "1.1.2"
    val basePluginVersion = "1.0.2"
    val jsoupVersion = "1.15.3"
    val specsVersion = "4.10.0"
    val playJsonVersion = "2.9.2"
    val zioVersion = "1.0.3"
  }

  import Versions._

  val db = Seq(
    "org.postgresql" % "postgresql" % postgresqlVersion,
    "mysql" % "mysql-connector-java" % mysqlVersion,
    "org.xerial" % "sqlite-jdbc" % sqliteVersion,
    "com.zaxxer" % "HikariCP" % hikariCPVersion,
    "com.typesafe.slick" %% "slick" %  slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" %  slickVersion,
    "io.github.nafg.slick-migration-api" %% "slick-migration-api" % "0.8.2"
  )

  val playJson = "com.typesafe.play" %% "play-json" % playJsonVersion

  val scalaLib = "org.scala-lang" % "scala-library" % scalaVersion

  val truerss = Seq(
    "io.github.truerss" % "content-extractor" % contentExtractorVersion,
    "io.github.truerss" %% "base" % basePluginVersion
  )

  val logs = Seq(
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )

  val jsoup = "org.jsoup" % "jsoup" % jsoupVersion

  val actorika = Seq(
    "io.github.truerss" %% "actorika" % "0.0.1.10-SNAPSHOT"
  )

  val scalaj = "org.scalaj" %% "scalaj-http" % scalajVersion

  val utils = Seq(
    scalaj,
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
   ("commons-validator" % "commons-validator" % commonValidatorVersion)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-logging", "commons-logging")
      .exclude("commons-digester", "commons-digester")
      .exclude("commons-collections", "commons-collections"),
    "com.typesafe" % "config" % configVersion,
    "com.github.scopt" %% "scopt" % scoptVersion,
    "org.java-websocket" % "Java-WebSocket" % jwsVersion
  )

  val zio = Seq(
    "dev.zio" %% "zio" % zioVersion
  )

  val tests = Seq(
    "org.specs2" %% "specs2-core" % specsVersion % Test,
    "org.specs2" %% "specs2-mock" % specsVersion % Test,

    "org.testcontainers" % "testcontainers" % "1.15.2" % Test,
    "org.testcontainers" % "mysql" % "1.15.2" % Test,
    "org.testcontainers" % "postgresql" % "1.15.2" % Test
  )

  val nettyL = Seq(
    "io.netty" % "netty-codec-http" % "4.1.60.Final",
    "io.netty" % "netty-codec-http2" % "4.1.60.Final",
    "com.github.fntz" %% "omhs-dsl" % "0.0.5",
    "com.github.fntz" %% "omhs-play-support" % "0.0.5",
    "org.scala-lang" % "scala-reflect" % scalaVersion % "compile"
  )

  val deps = db ++ actorika ++ truerss ++ logs ++
    Seq(jsoup, playJson) ++ utils ++ zio ++ tests ++ nettyL

}
