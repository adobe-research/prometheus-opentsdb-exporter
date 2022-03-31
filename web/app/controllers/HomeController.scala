package controllers
import javax.inject._

import play.api.mvc._

import models._


@Singleton
class HomeController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def status = Action {
    Ok(views.html.index(BuildInfo.version))
  }
}
