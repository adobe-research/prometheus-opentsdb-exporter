package controllers

import javax.inject._

import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Configuration, Logger}
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

  private val openTsdbUrl = configuration.getString("metrics.open_tsdb_url").get

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
          metrics.foreach { m =>
            Logger.info(s"Metrics: $m")
          }
          metrics.map { metric =>
            ws.url(s"$openTsdbUrl/api/query")
              .withRequestTimeout(5 seconds)    // TODO: extract as config options
              .withFollowRedirects(true)
              .post(metric.queryPayload)
              .transform(
                response =>
                  if ((response.status >= 200) && (response.status < 300)) {
                    metric -> response
                  } else {
                    val msg = s"OpenTSDB service error (${response.status}: ${response.statusText}\n${response.body})"
                    Logger.error(msg)
                    throw new Exception(msg)
                  },
                identity
              )
          }.map { _.map {
            case (metric, response) =>
              Logger.info(s"${metric.name}: \n ${response.json}")
            }
          }
          Ok("")
        case _ => InternalServerError
      }
    }
  }
}
