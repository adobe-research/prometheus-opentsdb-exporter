package actors

import akka.actor._
import play.api.Logging
import models.Metric


object MetricsRepoActor {
  def props(listener: Option[ActorRef] = None): Props = Props(new MetricsRepoActor(listener))
  def name = "metrics-repo"

  sealed trait MetricsRepoMessage
  case class RegisterMetrics(metrics: Seq[Metric]) extends MetricsRepoMessage
  case object GetMetrics extends MetricsRepoMessage
  case object ResetMetrics extends MetricsRepoMessage
  case class MetricsList(metrics: Seq[Metric]) extends MetricsRepoMessage
}

class MetricsRepoActor(listener: Option[ActorRef] = None) extends Actor with Logging {
  import actors.MetricsRepoActor._

  private [this] var metricsRepo: Map[String, Metric] = Map.empty

  override def receive = {
    case RegisterMetrics(metrics) =>
      metrics.foreach { metric =>
        logger.info(s"Registering metric: ${metric.name}")

        val newMetric = metricsRepo.get(metric.name).map { oldMetric =>
          val newQuery = oldMetric.query.copy(mappings = oldMetric.query.mappings ++ metric.query.mappings)
          oldMetric.copy(query = newQuery)
        }.getOrElse(metric)

        metricsRepo = metricsRepo + (newMetric.name -> newMetric)
      }

    case GetMetrics =>
      val metrics = MetricsList(metricsRepo.values.toList)
      sender ! metrics
      listener.foreach(_ ! metrics)

    case ResetMetrics =>
      logger.info("Resetting metrics.")
      metricsRepo = Map.empty
  }
}
