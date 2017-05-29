import org.scalatest.FunSpecLike

import akka.actor.ActorSystem
import akka.testkit._

import tools.StopSystemAfterAll

import models._
import actors.MetricsRepoActor


class MetricsRepoActorSpec extends TestKit(ActorSystem("test-system"))
  with FunSpecLike
  with StopSystemAfterAll {

  val testMetric = Metric(
    name = "test_metric",
    description = "Some test metric",
    metricType = "gauge",
    query = Query(
      start = "10s",
      end = None,
      mappings = Seq(
        Mapping(
          subQuery = SubQuery(aggregator = "avg", metric="test.metric"),
          prometheusTags = Some(Map("severity" -> "critical"))
        )
      )
    )
  )

  val testMetricNewMappings = Metric(
    name = "test_metric",
    description = "Some test metric",
    metricType = "gauge",
    query = Query(
      start = "10s",
      end = None,
      mappings = Seq(
        Mapping(
          subQuery = SubQuery(aggregator = "avg", metric="new.test.metric"),
          prometheusTags = Some(Map("severity" -> "normal"))
        )
      )
    )
  )

  describe("MetricsRepoActor") {
    it("should be able to add new metric") {
      import actors.MetricsRepoActor._

      val metricsRepo = system.actorOf(MetricsRepoActor.props(Some(testActor)), "test-001")

      metricsRepo ! RegisterMetrics(Seq(testMetric))
      expectNoMsg

      metricsRepo ! GetMetrics
      expectMsg(MetricsList(Seq(testMetric)))
    }

    it("should be able to add new mappings to existing metric") {
      import actors.MetricsRepoActor._

      val expectedMetric = Metric(
        name = "test_metric",
        description = "Some test metric",
        metricType = "gauge",
        query = Query(
          start = "10s",
          end = None,
          mappings = Seq(
            Mapping(
              subQuery = SubQuery(aggregator = "avg", metric="test.metric"),
              prometheusTags = Some(Map("severity" -> "critical"))
            ),
            Mapping(
              subQuery = SubQuery(aggregator = "avg", metric="new.test.metric"),
              prometheusTags = Some(Map("severity" -> "normal"))
            )
          )
        )
      )

      val metricsRepo = system.actorOf(MetricsRepoActor.props(Some(testActor)), "test-002")

      metricsRepo ! RegisterMetrics(Seq(testMetric))
      expectNoMsg

      metricsRepo ! RegisterMetrics(Seq(testMetricNewMappings))
      expectNoMsg

      metricsRepo ! GetMetrics
      expectMsg(MetricsList(Seq(expectedMetric)))
    }

    it("should be able to reset metrics repo") {
      import actors.MetricsRepoActor._

      val metricsRepo = system.actorOf(MetricsRepoActor.props(Some(testActor)), "test-003")

      metricsRepo ! RegisterMetrics(Seq(testMetric))
      expectNoMsg

      metricsRepo ! ResetMetrics
      expectNoMsg

      metricsRepo ! GetMetrics
      expectMsg(MetricsList(Seq()))
    }
  }
}
