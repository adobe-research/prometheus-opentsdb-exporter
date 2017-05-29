import Depedencies._
import Common._

name := """prometheus-opentsdb-exporter"""

val versionNo = settingKey[String]("Version number")
versionNo in ThisBuild := Process("cat VERSION").lines.head

val gitCommit = settingKey[String]("SHA of the last git commit.")
gitCommit in ThisBuild := Process("git rev-parse --short HEAD").lines.head

version in ThisBuild := s"${versionNo.value}-${gitCommit.value}"

lazy val web = SubProject("web")
  .enablePlugins(PlayScala)
  .settings(
    BuildInfo.settings,
    libraryDependencies ++= Seq(ws) ++ webAppDeps
  )

