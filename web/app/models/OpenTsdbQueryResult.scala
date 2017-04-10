package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DataPoint(
  timestamp: Long,
  value: Long
)

case class TsdbQueryResult(
  metric: String,
  tags: Map[String, String],
  dps: Seq[DataPoint]
) {
  def result: Long = ???
}

object TsdbQueryResult {
  implicit val tsdbQueryResult: Reads[TsdbQueryResult] = (
    (JsPath \ "metric").read[String] and
      (JsPath \ "tags").read[Map[String, String]]
  )
}
