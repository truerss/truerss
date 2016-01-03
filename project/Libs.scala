import sbt._

object Libs {
  object versions {
    val scalaVersion = "2.1.6"
    val scalajVersion = "2.2.0"
    val rountingExtVersion = "0.3.3"
    val h2Version = "1.3.173"
    val postgresqlVersion = "9.1-901-1.jdbc4"
    val mysqlVersion = "5.1.36"
    val sqliteVersion = "3.8.7"
    val romeVersion = "1.5.0"
    val commonValidatorVersion = "1.4.0"
    val ceVersion = "0.0.2"
    val shapelessVersion = "2.1.0"
    val sprayVersion = "1.3.3"
    val sprayJsonVersion = "1.3.2"
    val akkaVersion  = "2.3.9"
    val slickVersion = "2.1.0"
    val configVersion = "1.3.0"
    val scoptVersion = "3.3.0"
    val hikariCPVersion = "2.3.1"
    val jwsVersion = "1.3.0"
    val logbackVersion = "1.1.2"
    val log4j2Version = "1.0.0"
    val baseVersion = "0.0.5"
    val jsoupVersion = "1.8.3"
    val jodaVersion = "2.8.2"
    val scalaTestVersion = "3.0.0-M7"
    val javafakerVersion = "0.5"
    val jfairyVersion = "0.4.3"
  }

  import versions._

  val db = Seq(
    "com.h2database" % "h2" % h2Version,
    "postgresql" % "postgresql" % postgresqlVersion,
    "mysql" % "mysql-connector-java" % mysqlVersion,
    "org.xerial" % "sqlite-jdbc" % sqliteVersion,
    "com.zaxxer" % "HikariCP-java6" % hikariCPVersion,
    "com.typesafe.slick" %% "slick" %  slickVersion
  )

  val spray = Seq(
    "io.spray" %% "spray-routing-shapeless2" % sprayVersion,
    "io.spray" %% "spray-util" % sprayVersion,
    "io.spray" %% "spray-can"  % sprayVersion,
    ("io.spray" %% "spray-http" % sprayVersion)
      .exclude("org.scala-lang.modules", "scala-xml"),
    ("io.spray" %% "spray-httpx" % sprayVersion)
      .exclude("org.scala-lang.modules", "scala-xml"),
    "io.spray" %% "spray-json" % sprayJsonVersion,
    ("com.github.fntzr" %% "spray-routing-ext" % rountingExtVersion)
      .exclude("org.scala-lang", "scala-reflect")
  )

  val rome = Seq(
    "com.rometools" % "rome" % romeVersion,
    "com.rometools" % "rome-opml" % romeVersion
  )

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

  val akka = "com.typesafe.akka" %% "akka-actor" % akkaVersion

  val shapeless = "com.chuusai" %% "shapeless" % "2.1.0"

  val utils = Seq(
    "org.scalaj" %% "scalaj-http" % scalajVersion,
   ("commons-validator" % "commons-validator" % commonValidatorVersion)
      .exclude("commons-beanutils", "commons-beanutils")
      .exclude("commons-logging", "commons-logging")
      .exclude("commons-digester", "commons-digester")
      .exclude("commons-collections", "commons-collections"),
    "com.typesafe" % "config" % configVersion,
    "com.github.scopt" %% "scopt" % scoptVersion,
    "org.java-websocket" % "Java-WebSocket" % jwsVersion,
    "joda-time" % "joda-time" % jodaVersion
  )

  val tests = Seq(
    "org.scalatest" % "scalatest_2.11" % scalaTestVersion % "test",
    "org.scalactic" % "scalactic_2.11" % scalaTestVersion % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "io.spray" %% "spray-testkit" % sprayVersion % "test",
    "io.codearte.jfairy" % "jfairy" % "0.5.1" % "test"
  )

  val deps = db ++ spray ++ rome ++ truerss ++ logs ++
    Seq(jsoup, akka, shapeless) ++ utils ++ tests

}
