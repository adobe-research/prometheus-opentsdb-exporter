import Depedencies._
import Common._

name := """prometheus-opentsdb-exporter"""

val gitCommit = settingKey[String]("SHA of the last git commit.")
gitCommit in ThisBuild := Process("git rev-parse --short HEAD").lines.head

version in ThisBuild := s"1.0-${gitCommit.value}"

lazy val web = SubProject("web")
  .enablePlugins(PlayScala)
  .settings(
    BuildInfo.settings,
    libraryDependencies ++= Seq(ws) ++ webAppDeps
  )


