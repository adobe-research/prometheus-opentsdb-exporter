import play.sbt.PlayImport.guice
import sbt._

object Depedencies {
  val akkaVersion = "2.4.14"

  val commonDeps = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "org.specs2" %% "specs2-mock" % "3.8.9" % "test"
  )

  val loggingDeps = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.2"
  )

  val webAppDeps = Seq(
    // allows us to use bind[] APIs in the Module definitions
    "net.codingwell" %% "scala-guice" % "5.0.2",
  
    // helpers for bootstrap integration
    "com.adrianhurt" %% "play-bootstrap" % "1.6.1-P28-B4",
  
    // compass integration
    "org.webjars.bower" % "compass-mixins" % "0.12.10",
  
    // unit-testing support
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",

    // application loader
    guice
  )
}
