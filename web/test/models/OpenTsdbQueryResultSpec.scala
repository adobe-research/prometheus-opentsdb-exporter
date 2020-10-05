package models

import org.scalatestplus.play.PlaySpec

class OpenTsdbQueryResultSpec extends PlaySpec {

  "OpenTsdbQueryResult" should {
    "Santize tag names" in {
      val inputs = Map("abCD" -> "1", "123abc" -> "2", "@email" -> "3", "Unic\u00F6de" -> "4", "teach.me" -> "5")
      val expected = Map("abCD" -> "1", ":23abc" -> "2", ":email" -> "3", "Unic\u00F6de" -> "4", "teach_me" -> "5")
      TsdbQueryResult.sanitizeTags(inputs) must contain theSameElementsAs expected
    }
  }

}
