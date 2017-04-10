import scala.concurrent.Future

import play.api.mvc.Results._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError


package object models {
  type Tag = Map[String, String]

  implicit val JsPathWrites =
    Writes[JsPath](p => JsString(p.toString))

  implicit val ValidationErrorWrites =
    Writes[ValidationError] { e =>
      JsString(s"${e.message}(${e.args.mkString(",")})")
    }

  implicit val jsonValidateErrorWrites = (
    (JsPath \ "path").write[JsPath] and
    (JsPath \ "errors").write[Seq[ValidationError]]
    tupled
  )

  def oneOf[M](values: Set[M])(implicit reads: Reads[M]) =
    filter[M](ValidationError("error.oneOf",values))(values.contains)

  def respondWithErrors(errors: Seq[(JsPath, Seq[ValidationError])]) = {
    Future.successful(BadRequest(Json.toJson(errors)))
  }
}
