package models

import org.scalatestplus.play.PlaySpec

class OpenTsdbQueryResultSpec extends PlaySpec {

  "OpenTsdbQueryResult" should {
    "Santize tag names" in {
      val inputs = List("abCD", "123abc", "@email", "Unic\u00F6de", "teach.me")
      val expected = List("abCD", ":23abc", ":email", "Unic\u00F6de", "teach_me")
      inputs.map(TsdbQueryResult.sanitizeTagName) must contain theSameElementsInOrderAs expected
    }
  }

}
