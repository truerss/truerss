package truerss.util

import java.net.URI
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import scala.util.Try

object CommonImplicits {
  implicit class StringExt(val s: String) extends AnyVal {
    def normalize: String = {
      s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
    }

    def toIntOr(recover: Int): Int = {
      Try(s.toInt).getOrElse(recover)
    }

    def toUrl: URI = URI.create(s)
  }

  implicit class UrlExt(val x: URI) extends AnyVal {
    def toBase: String = {
      s"${x.getScheme}://${x.getHost}"
    }
  }

  implicit class LocalDateExt(ld: LocalDateTime) {
    def toDate: Date = {
      Date.from(ld.atZone(ZoneOffset.UTC).toInstant)
    }
  }

}




