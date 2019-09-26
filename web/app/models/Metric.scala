package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class RateOptions(
  counter: Boolean,
  counterMax: Long,
  resetValue: Long
)

case class Filter(
  filterType: String,
  tagk: String,
  filter: String,
  groupBy: Option[Boolean] = None
)


case class SubQuery(
  aggregator: String,
  metric: String,
  rate: Option[Boolean] = None,
  rateOptions: Option[RateOptions] = None,
  downsample: Option[String] = None,
  tags: Option[Map[String, String]] = None,
  filters: Option[Seq[Filter]] = None,
  explicitTags: Option[Boolean] = None
)

case class Mapping(
  subQuery: SubQuery,
  prometheusTags: Option[Map[String, String]] = None
)

case class Query(
  start: String,
  mappings: Seq[Mapping],
  end: Option[String] = None
)

case class Metric(
  name: String,
  description: String,
  metricType: String,
  query: Query
) {
  lazy val queryPayload: JsValue = {
    import Metric._
    Json.obj(
      "start" -> query.start,
      "end" -> query.end,
      "showQuery" -> true,
      "queries" -> query.mappings.map(m => Json.toJson(m.subQuery))
    )
  }
}

object Metric {
  implicit val rateOptionsFormat: Format[RateOptions] = (
    (JsPath \ "counter").format[Boolean] and
    (JsPath \ "counterMax").format[Long] and
    (JsPath \ "resetValue").format[Long]
  )(RateOptions.apply, unlift(RateOptions.unapply))

  implicit val filterFormat: Format[Filter] = (
    (JsPath \ "type").format[String] and
    (JsPath \ "tagk").format[String] and
    (JsPath \ "filter").format[String] and
    (JsPath \ "groupBy").formatNullable[Boolean]
  )(Filter.apply, unlift(Filter.unapply))

  implicit val subQueryFormat: Format[SubQuery] = (
    (JsPath \ "aggregator").format[String] and
    (JsPath \ "metric").format[String] and
    (JsPath \ "rate").formatNullable[Boolean] and
    (JsPath \ "rateOptions").formatNullable[RateOptions] and
    (JsPath \ "downsample").formatNullable[String] and
    (JsPath \ "tags").formatNullable[Map[String, String]] and
    (JsPath \ "filters").formatNullable[Seq[Filter]] and
    (JsPath \ "explicitTags").formatNullable[Boolean]
  )(SubQuery.apply, unlift(SubQuery.unapply))

  implicit val mappingFormat: Format[Mapping] = (
    (JsPath \ "subQuery").format[SubQuery] and
    (JsPath \ "prometheusTags").formatNullable[Map[String, String]]
  )(Mapping.apply, unlift(Mapping.unapply))

  implicit val queryFormat: Format[Query] = (
    (JsPath \ "start").format[String] and
    (JsPath \ "mappings").format[Seq[Mapping]](minLength[Seq[Mapping]](1)) and
    (JsPath \ "end").formatNullable[String]
  )(Query.apply, unlift(Query.unapply))

  implicit val metricFormat: Format[Metric] = (
    (JsPath \ "name").format[String] and
    (JsPath \ "description").format[String] and
    (JsPath \ "type").format[String] and
    (JsPath \ "query").format[Query]
  )(Metric.apply, unlift(Metric.unapply))
}

