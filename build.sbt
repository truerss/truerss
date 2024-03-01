import java.util.Date

import sbt.Package.ManifestAttributes
import Libs._
import Tasks._

name := "TrueRSS"

version := "1.0.7"

val setup = Seq(
  resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases",
  resolvers += "Sonatype OSS" at "https://s01.oss.sonatype.org/content/repositories/releases",
  resolvers += "JCenter" at "https://jcenter.bintray.com/",
  resolvers += "karussell_releases" at "https://github.com/karussell/mvnrepo",
  scalaVersion := "2.13.12",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:reflectiveCalls",
    "-unchecked",
    "-Xverify",
    "-Ydelambdafy:inline"
  ),
  javacOptions ++= Seq("-source", "21", "-target", "21")
)

// integrations testing

lazy val RealTest = config("real") extend(Test)

val dtos = project.in(file("dtos"))
  .settings(setup)
  .disablePlugins(sbtassembly.AssemblyPlugin)

val clients = project.in(file("clients"))
  .settings(setup)
  .settings(
    libraryDependencies ++= zio ++ logs ++ Seq(scalaj, playJson)
  ).dependsOn(dtos)
  .disablePlugins(sbtassembly.AssemblyPlugin)

val mainProject = Project("truerss", file("."))
  .configs(RealTest)
  .settings(inConfig(RealTest)(Defaults.testSettings) : _*)
  .settings(
    setup ++ Seq(installTask, buildCoffeeTask) ++ Seq(
    (Compile / compile) := (Compile / compile).dependsOn(buildCoffee).value,
    organization := "net.truerss",
    classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.ScalaLibrary,
    name := name.value,
    version := version.value,
    Test / parallelExecution := true,
    assembly / assemblyJarName := s"truerss_${version.value}.jar",
    assembly / mainClass := Some("Main"),
    assembly / assemblyMergeStrategy := {
      case x if x.contains(".conf") => MergeStrategy.concat
      case PathList(ps @ _*) =>
        if (ps.contains("log4j-provider.properties"))
          MergeStrategy.concat
        else
        if (ps.contains("MANIFEST.MF") || ps.contains("META-INF"))
          if (ps == List("MANIFEST.MF", "META-INF"))
            MergeStrategy.first
          else
            MergeStrategy.discard
        else
          MergeStrategy.first
      case _ =>
        MergeStrategy.first
    },
    assembly / test := {},
    assembly / target := file("."),
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      cp filter {x => x.data.getName.contains("scala-reflect")}
    },
    Test / publishArtifact := false,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    packageOptions := Seq(ManifestAttributes(("Built-By", s"${new Date()}"))),
    libraryDependencies ++= deps
  )
).dependsOn(dtos, clients % "real, test")
 .aggregate(dtos, clients)
