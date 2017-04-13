package controllers

import javax.inject._

import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.pattern.ask
import akka.util.Timeout

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Configuration, Logger}
import play.api.data.validation.ValidationError

import models._
import actors.MetricsRepoActor._
import services.MetricsRepoService


@Singleton
class MetricsController @Inject()(
  configuration: Configuration,
  metricsRepoService: MetricsRepoService,
  ws: WSClient
) extends Controller {

  private implicit val to: Timeout = 5 seconds

  private val openTsdbUrl = configuration.getString("metrics.openTsdb.url").get
  private val openTsdbTimeout = configuration.getLong("metrics.openTsdb.timeout").get

  private [this] def issueOpenTsdbRequest(metric: Metric): Future[Either[Throwable, (Metric, WSResponse)]] = {
    ws.url(s"$openTsdbUrl/api/query")
      .withRequestTimeout(openTsdbTimeout seconds)
      .withFollowRedirects(true)
      .post(metric.queryPayload)
      .transform(
        response =>
          if ((response.status >= 200) && (response.status < 300)) {
            Right(metric -> response)
          } else {
            val msg = s"OpenTSDB service error (${response.status}: ${response.statusText}\n${response.body})"
            throw new Exception(msg)
          },
        identity
      )

    .recover {
      case t => Left(t)
    }
  }

  def config = Action.async { implicit request =>
    metricsRepoService.metricsRepo.flatMap { mr =>
      (mr ? GetMetrics).mapTo[MetricsRepoMessage].map {
        case MetricsList(metrics) => Ok(Json.toJson(metrics))
        case _ => InternalServerError
      }
    }
  }

  def metrics = Action.async { implicit request =>
    metricsRepoService.metricsRepo.flatMap { mr =>
      (mr ? GetMetrics).mapTo[MetricsRepoMessage].map {
        case MetricsList(metrics) =>
          metrics.map(issueOpenTsdbRequest)
          .map { _.map {
            case Right((metric, response)) =>
              import TsdbQueryResult._
              response.json.validate[Seq[TsdbQueryResult]].fold(
                valid = queryResults => {
                  queryResults.flatMap { queryResult =>
                    queryResult.extractResults(metric)
                  }.foreach { promMetric =>
                    Logger.info("-----------")
                    Logger.info(s"Metric: ${promMetric.name}")
                    Logger.info(s"Value: ${promMetric.value}")
                    Logger.info("Tags:")
                    promMetric.tags.foreach {
                      case (k, v) =>
                        Logger.info(s"\t$k: $v")
                    }
                    Logger.info("-----------")
                  }
                },
                invalid = errors =>
                  Logger.info(Json.toJson(errors).toString)
              )
            }
          }
          Ok("")

        case _ => InternalServerError
      }
    }
  }
}
