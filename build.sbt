import java.util.Date

import sbt.Package.ManifestAttributes
import Libs._
import Tasks._

val setup = Seq(
  resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases",
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  resolvers += "JCenter" at "https://jcenter.bintray.com/",
  resolvers += "karussell_releases" at "https://github.com/karussell/mvnrepo",
  resolvers += Resolver.bintrayRepo("truerss", "maven"),
  scalaVersion := "2.12.4",
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
    "-Xfuture"
  ),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
)

lazy val mainProject = Project("truerss", file(".")).settings(
  setup ++ Seq(installTask, buildCoffeeTask) ++ Seq(
    (compile in Compile) := (compile in Compile).dependsOn(buildCoffee).value,
    organization := "net.truerss",
    name := "truerss",
    version := "0.0.3.4-pre.3",
    parallelExecution in Test := false,
    assemblyJarName in assembly := s"truerss-${version.value}.jar",
    mainClass in assembly := Some("truerss.Boot"),
    assemblyMergeStrategy in assembly := {
      case x if x.toString.contains(".conf") => MergeStrategy.concat
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
      case x =>
        MergeStrategy.first
    },
    test in assembly := {},
    fork in compile := true,
    publishArtifact in Test := false,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    packageOptions := Seq(ManifestAttributes(("Built-By", s"${new Date()}"))),
    libraryDependencies ++= deps
  )
)