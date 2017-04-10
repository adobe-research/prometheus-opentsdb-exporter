package actors

import actors.MetricsRepoActor.{GetMetrics, MetricsList, RegisterMetrics, ResetMetrics}
import akka.actor.{Actor, Props}
import play.api.Logger
import models.Metric


object MetricsRepoActor {
  def props: Props = Props[MetricsRepoActor]
  def name = "metrics-repo"

  sealed trait MetricsRepoMessage
  case class RegisterMetrics(metrics: Seq[Metric]) extends MetricsRepoMessage
  case object GetMetrics extends MetricsRepoMessage
  case object ResetMetrics extends MetricsRepoMessage
  case class MetricsList(metrics: Seq[Metric]) extends MetricsRepoMessage
}

class MetricsRepoActor extends Actor {
  private [this] var metricsRepo: Map[String, Metric] = Map.empty

  override def receive = {
    case RegisterMetrics(metrics) =>
      metrics.foreach { metric =>
        Logger.info(s"Registering metric: ${metric.name}")

        val newMetric = metricsRepo.get(metric.name).map { oldMetric =>
          val newQuery = oldMetric.query.copy(mappings = oldMetric.query.mappings ++ metric.query.mappings)
          oldMetric.copy(query = newQuery)
        }.getOrElse(metric)

        metricsRepo = metricsRepo + (newMetric.name -> newMetric)
      }

    case GetMetrics =>
      sender ! MetricsList(metricsRepo.values.toList)

    case ResetMetrics =>
      Logger.info("Resetting metrics.")
      metricsRepo = Map.empty
  }
}
