package models

sealed trait MetricType
case object GaugeMetricType extends MetricType
case object CounterMetricType extends MetricType
case object SummaryMetricType extends MetricType
case object HistoryMetricType extends MetricType

sealed trait TsdbAggregator
case object NoneTsdbAggregator extends TsdbAggregator
case object MinTsdbAggregator extends TsdbAggregator
case object MaxTsdbAggregator extends TsdbAggregator
case object MimminTsdbAggregator extends TsdbAggregator
case object MimmaxTsdbAggregator extends TsdbAggregator
case object FirstTsdbAggregator extends TsdbAggregator
case object LastTsdbAggregator extends TsdbAggregator
case object AvgTsdbAggregator extends TsdbAggregator
case object CountTsdbAggregator extends TsdbAggregator
case object DevTsdbAggregator extends TsdbAggregator
case object SumTsdbAggregator extends TsdbAggregator
case object ZimSumTsdbAggregator extends TsdbAggregator
case object P50TsdbAggregator extends TsdbAggregator
case object P75TsdbAggregator extends TsdbAggregator
case object P90TsdbAggregator extends TsdbAggregator
case object P95TsdbAggregator extends TsdbAggregator
case object P99TsdbAggregator extends TsdbAggregator
case object P999TsdbAggregator extends TsdbAggregator
case object EP50R3TsdbAggregator extends TsdbAggregator
case object EP50R7TsdbAggregator extends TsdbAggregator
case object EP75R3TsdbAggregator extends TsdbAggregator
case object EP75R7TsdbAggregator extends TsdbAggregator
case object EP90R3TsdbAggregator extends TsdbAggregator
case object EP90R7TsdbAggregator extends TsdbAggregator
case object EP95R3TsdbAggregator extends TsdbAggregator
case object EP95R7TsdbAggregator extends TsdbAggregator
case object EP99R3TsdbAggregator extends TsdbAggregator
case object EP99R7TsdbAggregator extends TsdbAggregator
case object EP999R3TsdbAggregator extends TsdbAggregator
case object EP999R7TsdbAggregator extends TsdbAggregator

case object TsdbAggregator extends TsdbAggregator


case class TsdbMappingParams (
  aggregator: TsdbAggregator,
  rated: Boolean,
  metric: String,
  tags: Map[String, String]
)

case class PrometheusMappingParams (
  tags: Map[String, String]
)

case class Mapping(
  tsdb: TsdbMappingParams,
  prometheus: PrometheusMappingParams
)

case class Metric(
  name: String,
  description: String,
  metricType: MetricType,
  mappings: Set[Mapping]
)

