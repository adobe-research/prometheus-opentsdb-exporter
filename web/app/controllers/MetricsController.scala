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

  private def issueOpenTsdbRequest(metric: Metric): Future[Either[Throwable, Seq[PrometheusMetric]]] = {
    def parseResponse(response: WSResponse): Seq[PrometheusMetric] = {
      import TsdbQueryResult._
      response.json.validate[Seq[TsdbQueryResult]].fold(
        valid = queryResults =>
          queryResults.flatMap(_.extractResults(metric)),
        invalid = errors =>
          throw new Exception(s"JSON parse errors: ${Json.toJson(errors).toString}")
      )
    }

    ws.url(s"$openTsdbUrl/api/query")
      .withRequestTimeout(openTsdbTimeout seconds)
      .withFollowRedirects(true)
      .post(metric.queryPayload)
      .transform(
        response =>
          if ((response.status >= 200) && (response.status < 300)) {
            Right(parseResponse(response))
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

  def config: Action[AnyContent] = Action.async { implicit request =>
    metricsRepoService.metricsRepo.flatMap { mr =>
      (mr ? GetMetrics).mapTo[MetricsRepoMessage].map {
        case MetricsList(metrics) => Ok(Json.toJson(metrics))
        case _ => InternalServerError
      }
    }
  }

  def metrics: Action[AnyContent] = Action.async { implicit request =>
    metricsRepoService.metricsRepo.flatMap { mr =>
      (mr ? GetMetrics).mapTo[MetricsRepoMessage].flatMap {
        case MetricsList(metrics) =>
          Future.sequence(metrics.map(issueOpenTsdbRequest))
            .map { results =>
              val (errors, promMetrics) = results.partition(_.isLeft)

              // log the errors
              for (Left(t) <- errors) Logger.info(t.getMessage)

              // generate the output metrics
              val pmGroups = (for (Right(pm) <- promMetrics) yield pm).map { pmGroup =>
                // all metrics in a metric group share the same name, description and type
                val first = pmGroup.head
                (first.name, first.description, first.metricType, pmGroup)
              }

              val output = views.txt.metrics(pmGroups).body.split("\n")
                .map(_.trim)          // trim leading/trailing spaces
                .filterNot(_.isEmpty) // get rid of all the empty lines
                .mkString("\n")

              Ok(output)
            }

        case _ => Future.successful(InternalServerError)
      }
    }
  }
}
