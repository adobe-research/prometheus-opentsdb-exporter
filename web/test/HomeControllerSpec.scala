import org.scalatestplus.play._

import play.api.test._
import play.api.test.Helpers._


class HomeControllerSpec extends PlaySpec
  with OneAppPerTest {
  def statusRequest =
    FakeRequest(GET, "/")

  "HomeController" should {
    "return the version number in the status page" in {
      route(app, statusRequest).foreach { result =>
        status(result) mustBe OK

        contentAsString(result).contains("version: ") mustBe true
      }
    }
  }
}
