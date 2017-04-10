package controllers
import javax.inject._

import play.api.mvc._

import models._


@Singleton
class HomeController @Inject() extends Controller {

  def status = Action {
    Ok(s"OpenTSDB exporter for Prometheus (v${BuildInfo.version})")
  }
}
