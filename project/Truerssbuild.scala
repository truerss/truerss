import java.util.Date

import org.sbtidea.SbtIdeaPlugin._
import sbt.Keys._
import sbt.Package.ManifestAttributes
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import spray.revolver.RevolverPlugin._

object Truerssbuild extends Build {
  import Libs._
  import Tasks._

  val setting = Revolver.settings ++ Seq(
    resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "JCenter" at "http://jcenter.bintray.com/",
    resolvers += "karussell_releases" at "https://github.com/karussell/mvnrepo",
    resolvers += Resolver.bintrayRepo("truerss", "maven"),
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-encoding", "UTF-8",
      "-feature",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:reflectiveCalls",
      "-unchecked",
      "-Xcheckinit",
      "-Xverify",
      "-Xfuture"
    )
  )

  lazy val mainProject = Project(
    id = "truerss",
    base = file("."),
    settings = net.virtualvoid.sbt.graph.Plugin.graphSettings ++ setting ++ Seq(installTask, buildCoffeeTask) ++ Seq(
      (compile in Compile) <<= (compile in Compile).dependsOn(buildCoffee),
      organization := "net.truerss",
      name := "truerss",
      ideaExcludeFolders := ".idea" :: ".idea_modules" :: Nil,
      version := "0.0.1",
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
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      publishArtifact in Test := false,
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
      packageOptions := Seq(ManifestAttributes(("Built-By", s"${new Date()}"))),
      libraryDependencies ++= deps
    )
  )


}