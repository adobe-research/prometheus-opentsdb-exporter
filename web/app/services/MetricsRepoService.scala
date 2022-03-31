package services

import scala.concurrent.duration._
import java.io.{File, FileInputStream}
import javax.inject._
import akka.actor.{ActorNotFound, ActorSystem}
import akka.util.Timeout
import play.api.libs.json._
import play.api.{Configuration, Logging}
import models.Metric
import actors.MetricsRepoActor
import actors.MetricsRepoActor.{RegisterMetrics, ResetMetrics}


@Singleton
class MetricsRepoService @Inject()(
  configuration: Configuration,
  system: ActorSystem
) extends Logging {
  private implicit val to: Timeout = 5 seconds

  private val metricsDir = configuration.get[String]("metrics.dir")

  private implicit val ec = system.dispatcher

  private def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList.sortBy(_.getAbsolutePath)
    } else {
      logger.warn(s"Metrics dir not found: $dir")
      logger.info(s"Working dir: ${new File(".").getAbsolutePath}")
      List[File]()
    }
  }

  lazy val metricsRepo = {
    logger.info(s"Initializing the metrics repo.")
    system.actorSelection(s"${MetricsRepoActor.name}")
      .resolveOne()
      .recover {
        case ActorNotFound(_) =>
          system.actorOf(MetricsRepoActor.props(), MetricsRepoActor.name)
      }
  }

  def reloadMetrics(): Unit = {
    metricsRepo.foreach { mr =>
      logger.info("Loading metrics definitions.")

      mr ! ResetMetrics

      getListOfFiles(metricsDir).foreach { f =>
        logger.info(s"Loading metrics definitions from: ${f.getAbsolutePath}")

        Json.parse(new FileInputStream(f)).validate[Seq[Metric]].fold(
          valid = metrics => {
            logger.info("Metrics definitions parsed and validating. Reloading...")
            mr ! RegisterMetrics(metrics)
          },
          invalid = errors =>
            logger.error(errors.mkString("\n"))
        )
      }
    }
  }

  reloadMetrics()
}
