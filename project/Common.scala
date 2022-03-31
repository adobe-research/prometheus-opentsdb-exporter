import sbt._
import Keys._
import Depedencies._
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy


object Common {
  val commonSettings = Seq(
    organization := "com.adobe",

    scalaVersion := "2.12.15",

    scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked",
      "-language:reflectiveCalls",
      "-language:postfixOps",
      "-language:implicitConversions"),

    libraryDependencies ++= commonDeps,

    fork in run := true,

    assemblyMergeStrategy in assembly := {
      case manifest if manifest.contains("MANIFEST.MF") =>
        // We don't need manifest files since sbt-assembly will create one
        MergeStrategy.discard
      case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
        // Keep the content for all reference-overrides.conf file
        MergeStrategy.concat
      case "application.conf" =>
        MergeStrategy.last
      case "logback.xml" =>
        MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
)

def SubProject(name: String) = {
    Project(name, file(name))
      .settings(commonSettings:_*)
  }
}
