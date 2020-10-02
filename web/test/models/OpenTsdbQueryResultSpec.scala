/*
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2020 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 *
 */

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
