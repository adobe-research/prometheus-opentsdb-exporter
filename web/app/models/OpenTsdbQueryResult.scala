package models

import scala.util.Try

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DataPoint(
  timestamp: Long,
  value: Double
)

case class PrometheusMetric(
  name: String,
  description: String,
  metricType: String,
  tags: Map[String, String],
  value: Double
) {
//  def asOutputString: String = {
//    val tagsAsString = tags.map(s"$k=$v")
//    s"$name "
//  }
}

case class TsdbQueryResult(
  metric: String,
  tags: Map[String, String],
  dps: Seq[DataPoint],
  subQuery: SubQuery
) {
  private [this] lazy val latestDataPoint: Option[DataPoint] = Try(dps.maxBy(_.timestamp)).toOption

  private def mergeTags(prometheusTags: Option[Map[String, String]]): Option[Map[String, String]] = tags match {
    case _ if tags.isEmpty => prometheusTags
    case _ => Try(prometheusTags.getOrElse(Map[String, String]()) ++ tags).toOption
  }

  def extractResults(metric: Metric): Option[PrometheusMetric] = {
    for {
      mapping <- metric.query.mappings.find(_.subQuery.copy(filters = None) == subQuery.copy(filters = None))
      tags <- mergeTags(mapping.prometheusTags)
      dp <- latestDataPoint
    } yield PrometheusMetric(metric.name, metric.description, metric.metricType, TsdbQueryResult.sanitizeTags(tags), dp.value)
  }
}

object TsdbQueryResult {
  import Metric.rateOptionsFormat

  implicit val subQueryFormat: Reads[SubQuery] = (
    (JsPath \ "metric").read[String] and
    (JsPath \ "aggregator").read[String] and
    (JsPath \ "rate").readNullable[Boolean] and
    (JsPath \ "rateOptions").readNullable[RateOptions] and
    (JsPath \ "downsample").readNullable[String] and
    (JsPath \ "tags").readNullable[Map[String, String]]
  )(
    (metric: String,
     aggregator: String,
     rate: Option[Boolean],
     rateOptions: Option[RateOptions],
     downsample: Option[String],
     tags: Option[Map[String, String]]) => {

      val tagPattern = ".+\\((.+)\\)".r
      val _tags: Option[Map[String, String]] = tags.map { _.map {
        case (k, v) =>
          val tagPattern(_v) = v
          k -> _v
      }}

      SubQuery(
        metric = metric,
        aggregator = aggregator,
        rate = rate,
        rateOptions = rateOptions,
        downsample = downsample,
        tags = _tags,
        filters = None,
        explicitTags = None
      )
    }
  )

  implicit val tsdbQueryResultReads: Reads[TsdbQueryResult] = (
    (JsPath \ "metric").read[String] and
    (JsPath \ "tags").read[Map[String, String]] and
    (JsPath \ "dps").read[Map[String, Double]] and
    (JsPath \ "query").read[SubQuery]
  )(
    (metric: String, tags: Map[String, String], dps: Map[String, Double], query: SubQuery) =>
      TsdbQueryResult(
        metric = metric,
        tags = tags,
        dps = dps.map {
          case (ts, v) => DataPoint(ts.toLong, v)
        }.toSeq,
        subQuery = query
      )
  )

  def sanitizeTags(prometheusTags: Map[String, String]): Map[String, String] = {
    def sanitizeTagName(label: String): String = label
      .replaceFirst("^[^\\p{L}_:]", ":")
      .replaceAll("[^\\p{L}0-9_:]+", "_")
    prometheusTags.map { case (key, value) => (sanitizeTagName(key), value) }
  }
}
