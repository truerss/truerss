package net.truerss.services

import org.specs2.mutable.Specification
import truerss.db.validation.{SourceUrlValidator => S}

class SourceUrlValidatorTest extends Specification {

  "validator" should {
    "isValid method" in {
      Map(
        "" -> false,
        "application/rss+xml" -> true,
        "application/rdf+xml" -> true,
        "application/atom+xml" -> true,
        "application/xml" -> true,
        "text/xml" -> true,
        "application/javascript" -> false
      ) map { case (k, v) =>
        S.isValid(k) ==== v
      }
      success
    }
  }

}
