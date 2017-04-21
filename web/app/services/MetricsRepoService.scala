package services

import scala.concurrent.duration._
import java.io.{File, FileInputStream}
import javax.inject._

import akka.actor.{ActorNotFound, ActorSystem}
import akka.util.Timeout
import play.api.libs.json._
import play.api.{Configuration, Logger}
import models.Metric
import actors.MetricsRepoActor
import actors.MetricsRepoActor.{RegisterMetrics, ResetMetrics}


@Singleton
class MetricsRepoService @Inject()(
  configuration: Configuration,
  system: ActorSystem
) {
  private implicit val to: Timeout = 5 seconds

  private val metricsDir = configuration.getString("metrics.dir").get
  private val refreshTime = configuration.getLong("metrics.configRefreshTime").get

  private implicit val ec = system.dispatcher

  private def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  lazy val metricsRepo = {
    Logger.info(s"Initializing the metrics repo.")
    system.actorSelection(s"${MetricsRepoActor.name}")
      .resolveOne()
      .recover {
        case ActorNotFound(_) =>
          system.actorOf(MetricsRepoActor.props, MetricsRepoActor.name)
      }
  }

  system.scheduler.schedule(0 second, refreshTime second) {
    metricsRepo.foreach { mr =>
      Logger.info("Loading metrics definitions.")

      mr ! ResetMetrics

      getListOfFiles(metricsDir).foreach { f =>
        Logger.info(s"Loading metrics definitions from: ${f.getAbsolutePath}")

        Json.parse(new FileInputStream(f)).validate[Seq[Metric]].fold(
          valid = metrics =>
            mr ! RegisterMetrics(metrics)
          ,
          invalid = errors =>
            Logger.error(errors.mkString("\n"))
        )
      }
    }
  }
}
