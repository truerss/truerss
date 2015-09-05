import java.net.URL

import java.io.{FileWriter, File}
import scala.io.Source

import sbt._
import sbt.Keys._
import spray.revolver.RevolverPlugin._
import org.sbtidea.SbtIdeaPlugin._
import sbtassembly.AssemblyPlugin.autoImport._
import sbt.Package.ManifestAttributes

object Truerssbuild extends Build {

  val setting = Revolver.settings ++ Seq(
    scalacOptions ++= Seq("-Xlog-free-terms", "-deprecation", "-feature"),
    resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "JCenter" at "http://jcenter.bintray.com/",
    resolvers += "karussell_releases" at "https://github.com/karussell/mvnrepo",
    resolvers += Resolver.bintrayRepo("truerss", "maven"),
    scalaVersion := "2.10.5",
    scalacOptions ++= Seq("-Xlog-free-terms", "-deprecation", "-feature",
      "-encoding", "UTF-8",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:reflectiveCalls",
      "-deprecation",
      "-unchecked",
      "-Xcheckinit",
      "-Xverify",
      "-Xfuture")
  )

  val sprayVersion = "1.3.3"
  val akkaVersion  = "2.3.9"
  val slickVersion = "2.1.0"
  val scalazVersion = "7.1.3"

  val install = TaskKey[Unit]("install", "install all dependencies for web ui")
  val installTask = install := {
    val ss: TaskStreams = streams.value
    val jsUrls = List(
      "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/js/bootstrap.min.js",
      "https://embeddedjavascript.googlecode.com/files/ejs_production.js",
      "http://code.jquery.com/jquery-2.1.3.min.js",
      "https://raw.githubusercontent.com/istvan-ujjmeszaros/bootstrap-touchspin/master/src/jquery.bootstrap-touchspin.js",
      "https://raw.githubusercontent.com/fntzr/sirius/master/jquery_adapter.min.js",
      "https://raw.githubusercontent.com/fntzr/sirius/master/sirius.min.js",
      "https://raw.githubusercontent.com/lodash/lodash/3.0.1/lodash.min.js"
    )
    val cssUrls = List(
      "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css",
      "http://yui.yahooapis.com/pure/0.5.0/pure-min.css",
      "https://raw.githubusercontent.com/istvan-ujjmeszaros/bootstrap-touchspin/master/src/jquery.bootstrap-touchspin.css"
    )

    val currentPath = new File(".").getAbsolutePath
    val jsPath = s"${currentPath}/src/main/resources/javascript/".replaceAll("""\/\.\/""", "/")
    val cssPath = s"${currentPath}/src/main/resources/css/".replaceAll("""\/\.\/""", "/")

    val pnotifyJs = List(
      "https://raw.githubusercontent.com/sciactive/pnotify/master/pnotify.core.js",
      "https://raw.githubusercontent.com/sciactive/pnotify/master/pnotify.buttons.js",
      "https://raw.githubusercontent.com/sciactive/pnotify/master/pnotify.nonblock.js"
    )

    val pnotifyCss = List(
      "https://raw.githubusercontent.com/sciactive/pnotify/master/pnotify.core.css",
      "https://raw.githubusercontent.com/sciactive/pnotify/master/pnotify.buttons.css"
    )

    def downloadNotify(seq: List[String], fileName: String, dir: String): Unit = {
      val file = s"${dir}${fileName}"
      val pf = new File(file)
      if (pf.exists()) {
        pf.delete()
      }
      val fw = new FileWriter(file, true)
      ss.log.info(s"download ${fileName}")
      seq.foreach {
        case x =>
          val content = Source.fromURL(new URL(x)).mkString
          fw.write(content)
      }
      fw.close()
    }

    downloadNotify(pnotifyCss, "pnotify.custom.css", cssPath)
    downloadNotify(pnotifyJs, "pnotify.custom.js", jsPath)

    jsUrls.foreach {
      case x =>
        val name = x.split("""/""").last
        downloadNotify(List(x), name, jsPath)
    }

    cssUrls.foreach {
      case x =>
        val name = x.split("""/""").last
        downloadNotify(List(x), name, cssPath)
    }

    ss.log.info("done")
  }

  val buildCoffee = TaskKey[Unit]("jsbuild", "compile coffeescript")
  val buildCoffeeTask = buildCoffee := {
    val ss: TaskStreams = streams.value
    ss.log.info("Compile Coffeescript")
    import sys.process._

    val currentPath = new File(".").getAbsolutePath()
    val appDir = s"${currentPath}/src/main/resources/javascript/app/".replaceAll("""\/\.\/""", "/")
    val controllers = new File(s"${appDir}/controllers/").listFiles().toList
    val models = new File(s"${appDir}/models/").listFiles().toList
    val ext = new File(s"${appDir}/ext/").listFiles().toList
    val renderer = List(new File(s"${appDir}/renderer.coffee"))
    val app = List(new File(s"${appDir}/app.coffee"))

    val result = ext ::: renderer ::: models ::: controllers ::: app

    val fls = result.mkString(" ")

    val outFile = s"$appDir/app.js"
    val fo = new File(outFile)
    if (fo.exists()) {
      fo.delete()
    }

    "rake"!

    ss.log.info("done")
  }


  import java.util.Date

  lazy val mainProject = Project(
    id = "truerss",
    base = file("."),
    settings = Project.defaultSettings ++ setting ++ Seq(installTask, buildCoffeeTask) ++ Seq(
     // (compile in Compile) <<= (compile in Compile).dependsOn(buildCoffee),
      organization := "net.truerss",
      name := "truerss",
      ideaExcludeFolders := ".idea" :: ".idea_modules" :: Nil,
      version := "0.0.3",
      parallelExecution in Test := false,
      assemblyJarName in assembly := "truerss-0.0.3.jar",
      mainClass in assembly := Some("truerss.Boot"),
      mergeStrategy in assembly := {
        case x if x.toString.contains(".conf") => MergeStrategy.concat
        case PathList(ps @ _*) =>
          if (ps.contains("MANIFEST.MF") || ps.contains("META-INF"))
            if (ps == List("MANIFEST.MF", "META-INF"))
              MergeStrategy.first
            else
              MergeStrategy.discard
          else
            MergeStrategy.first
        case x =>
          MergeStrategy.first
      },
      test in assembly := {},
      fork in compile := true,
      publishArtifact in Test := false,
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
      packageOptions := Seq(ManifestAttributes(
        ("Built-By", s"${new Date()}"))),
      libraryDependencies ++= Seq(
        "org.scalaj" %% "scalaj-http" % "1.1.5",
        "com.github.fntzr"  %% "spray-routing-ext" % "0.2.2",

        "com.h2database" % "h2" % "1.3.173",
        "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
        "org.xerial" % "sqlite-jdbc" % "3.8.7",

        "com.rometools" % "rome" % "1.5.0",
        "com.rometools" % "rome-opml" % "1.5.0",
        "commons-validator" % "commons-validator" % "1.4.0",
        "commons-io" % "commons-io" % "2.4",

        "org.scala-lang" % "scala-library" % "2.11.7",
        "com.github.truerss" % "content-extractor" % "0.0.1",
        "io.spray" %% "spray-routing" % sprayVersion,
        "io.spray" %% "spray-util" % sprayVersion,
        "io.spray" %% "spray-can"  % sprayVersion,
        "io.spray" %% "spray-http" % sprayVersion,
        "io.spray" %% "spray-httpx" % sprayVersion,
        "io.spray" %% "spray-io" % sprayVersion,
        "io.spray" %% "spray-caching" % sprayVersion,
        "io.spray" %% "spray-json" % "1.3.2",
        "io.spray" %%  "spray-testkit" % sprayVersion % "test",

        "com.typesafe.akka" %% "akka-actor" % akkaVersion,

        "com.typesafe" % "config" % "1.3.0",
        "com.github.scopt" %% "scopt" % "3.3.0",
        "com.typesafe.slick" %% "slick" %  slickVersion,
        "com.zaxxer" % "HikariCP-java6" % "2.3.1",
        "org.java-websocket" % "Java-WebSocket" % "1.3.0",

        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "ch.qos.logback" % "logback-classic" % "1.1.2",

        "com.github.truerss" %% "base" % "0.0.2",

        "org.scalaz" %% "scalaz-core" % "7.1.3",  
        "org.jsoup" % "jsoup" % "1.8.3",
        "commons-validator" % "commons-validator" % "1.4.0",
        "joda-time" % "joda-time" % "2.8.2",

        "org.scalatest" % "scalatest_2.10" % "3.0.0-M7" % "test",
        "com.github.tomakehurst" % "wiremock" % "1.46" % "test",
        "com.github.javafaker" % "javafaker" % "0.5" % "test",
        "org.scalactic" % "scalactic_2.10" % "3.0.0-M7" % "test",
        "io.codearte.jfairy" % "jfairy" % "0.4.3" % "test"
      )
    )
  )


}