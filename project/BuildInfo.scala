import sbt._
import Keys._

object BuildInfo {
  val properties = settingKey[Map[String, String]]("Build info as key-value map")
  val infoFile = taskKey[Seq[File]]("Create build info file.")

  val settings = Seq(
    properties := Map.empty,
    properties += "version" -> version.value,
    infoFile := {
      makeInfoFile(
        (sourceManaged in Compile).value / "BuildInfo.scala",
        properties.value,
        streams.value.log
      )
    },
    sourceGenerators in Compile += infoFile.taskValue
  )

  def makeInfoFile(file: File, props: Map[String, String], log: Logger) = {
    val lines = for((key, value) <- props) yield s"""val $key = "$value" """

    val content = s"""package models
      |
      |object BuildInfo {
      |  ${lines.mkString("\n  ")}
    |}\n""".stripMargin

    log.info("Generating build info file")
    IO.write(file, content)
    Seq(file)
  }
}
