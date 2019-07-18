import org.scalatestplus.play._
import org.scalatest.TestData

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.test._
import play.api.Application

import models._
import tools.OpenTsdbWSMock
import actors.MetricsRepoActor

class MetricsControllerSpec extends PlaySpec
  with OneAppPerTest {

  override def newAppForTest(testData: TestData): Application = {
    new GuiceApplicationBuilder()
      .overrides(bind[WSClient].to[SimpleOpenTsdbWSMock])
      .build()
  }

  def configRequest =
    FakeRequest(GET, "/config")

  def reloadRequest =
    FakeRequest(POST, "/reload")

  def metricsRequest =
    FakeRequest(GET, "/metrics")

  "MetricsController" should {
    "return the metrics configuration on the '/config' endpoint" in {
      route(app, configRequest).foreach { result =>
        status(result) mustBe OK

        Json.fromJson[Seq[Metric]](contentAsJson(result)).foreach { metrics =>
          metrics.size mustBe 2

          metrics(0).name mustBe "test_app_metrics_one"
          metrics(1).name mustBe "test_app_metrics_two"
        }
      }
    }

    "reload the metrics configuration on the '/reload' endpoint" in {
      import actors.MetricsRepoActor._

      app.actorSystem
        .actorSelection(s"${MetricsRepoActor.name}")
        .resolveOne()
        .map { mr =>
          mr ! ResetMetrics

          route(app, reloadRequest).foreach { result =>
            status(result) mustBe OK

            Json.fromJson[Seq[Metric]](contentAsJson(result)).foreach { metrics =>
              metrics.size mustBe 2

              metrics(0).name mustBe "test_app_metrics_one"
              metrics(1).name mustBe "test_app_metrics_two"
            }
          }
        }.recover {
          case _ => fail("unable to reset metrics")
        }
    }

    "expose Prometheus-formatted metrics on the '/metrics' endpoint" in {
      route(app, metricsRequest).foreach { result =>
        status(result) mustBe OK

        val output = contentAsString(result).split("\n").toList
        output must contain theSameElementsInOrderAs List(
          "# HELP test_app_metrics_one TestApp metrics: one",
          "# TYPE test_app_metrics_one counter",
          """test_app_metrics_one{severity="critical",escalation="pagerduty",role="developer"} 15""",
          "# HELP test_app_metrics_two TestApp metrics: two",
          "# TYPE test_app_metrics_two counter",
          """test_app_metrics_two{severity="critical",escalation="pagerduty",role="developer"} 10"""
        )
      }
    }
  }
}

class SimpleOpenTsdbWSMock extends OpenTsdbWSMock {
  override def responsePayload = {
    val payloadRespOne =
      """
        |[
        |  {
        |    "metric": "test.app.metrics.one",
        |    "tags": {
        |      "role": "developer"
        |    },
        |    "aggregateTags": [],
        |    "query": {
        |      "aggregator": "avg",
        |      "metric": "test.app.metrics.one",
        |      "tsuids": null,
        |      "downsample": null,
        |      "rate": false,
        |      "filters": [
        |        {
        |          "tagk": "role",
        |          "filter": "developer",
        |          "group_by": true,
        |          "type": "literal_or"
        |        }
        |      ],
        |      "rateOptions": null,
        |      "tags": {
        |        "role": "literal_or(developer)"
        |      }
        |    },
        |    "dps": {
        |      "1496131203": 15,
        |      "1496131204": 15,
        |      "1496131205": 15,
        |      "1496131206": 15,
        |      "1496131207": 15,
        |      "1496131208": 15,
        |      "1496131209": 15,
        |      "1496131211": 15,
        |      "1496131212": 15
        |    }
        |  }
        |]
      """.stripMargin

    val payloadRespTwo =
      """
        |[
        |  {
        |    "metric": "test.app.metrics.two",
        |    "tags": {
        |      "role": "developer"
        |    },
        |    "aggregateTags": [],
        |    "query": {
        |      "aggregator": "avg",
        |      "metric": "test.app.metrics.two",
        |      "tsuids": null,
        |      "downsample": null,
        |      "rate": false,
        |      "filters": [
        |        {
        |          "tagk": "role",
        |          "filter": "developer",
        |          "group_by": true,
        |          "type": "literal_or"
        |        }
        |      ],
        |      "rateOptions": null,
        |      "tags": {
        |        "role": "literal_or(developer)"
        |      }
        |    },
        |    "dps": {
        |      "1496132481": 10,
        |      "1496132482": 10,
        |      "1496132483": 10,
        |      "1496132484": 10,
        |      "1496132485": 10,
        |      "1496132487": 10,
        |      "1496132488": 10,
        |      "1496132489": 10
        |    }
        |  }
        |]
      """.stripMargin

    Map(
      "test.app.metrics.one" -> Json.parse(payloadRespOne),
      "test.app.metrics.two" -> Json.parse(payloadRespTwo)
    )
  }
}

