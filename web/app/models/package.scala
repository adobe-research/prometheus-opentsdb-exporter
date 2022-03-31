import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Results._

import scala.concurrent.Future


package object models {
  type Tag = Map[String, String]

  implicit val JsPathWrites =
    Writes[JsPath](p => JsString(p.toString))

  implicit val JsonValidationErrorWrites =
    Writes[JsonValidationError] { e =>
      JsString(s"${e.message}(${e.args.mkString(",")})")
    }

  implicit val jsonValidateErrorWrites = (
    (JsPath \ "path").write[JsPath] and
    (JsPath \ "errors").write[Seq[JsonValidationError]]
    tupled
  )

  def oneOf[M](values: Set[M])(implicit reads: Reads[M]) =
    filter[M](JsonValidationError("error.oneOf",values))(values.contains)

  def respondWithErrors(errors: Seq[(JsPath, Seq[JsonValidationError])]) = {
    Future.successful(BadRequest(Json.toJson(errors)))
  }
}
