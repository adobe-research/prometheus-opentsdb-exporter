package tools

import scala.concurrent.duration.Duration
import scala.concurrent.Future

import play.api.http.{HeaderNames, Writeable}
import play.api.libs.ws._
import play.api.libs.json._
import play.api.http.Status._

import org.specs2.mock.Mockito


abstract class OpenTsdbWSMock extends Mockito with WSClient {
  private val request = mock[WSRequest]
  private val response = mock[WSResponse]

  private var metrics: List[String] = List.empty

  private val urls:collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

  request.withRequestTimeout(any[Duration]) returns request
  request.withFollowRedirects(any[Boolean]) returns request

  response.status returns OK
  response.header(HeaderNames.CONTENT_TYPE) returns Some("application/json;charset=UTF-8")
  response.json answers { _ => this.synchronized {
    val payload = responsePayload(metrics.head)
    metrics = metrics.tail
    payload
  }}

  request.post(anyString)(any[Writeable[String]]) answers { args => this.synchronized {
    val payload = args.asInstanceOf[Array[Object]](0).asInstanceOf[JsValue]
    val metric = (payload \ "queries") (0) \ "metric" match {
      case JsDefined(m) => m.toString.replace("\"", "")
      case _ => ""
    }

    metrics = metrics ++ List(metric)

    Future.successful(response)
  }}

  def url(url: String): WSRequest = {
    urls += url
    request
  }

  def underlying[T]: T = this.asInstanceOf[T]

  protected def responsePayload: Map[String, JsValue]

  override def close(): Unit = ()
}
