package truerss.util

/**
 * Created by mike on 2.8.15.
 */
object Util {
  implicit class StringExt(s: String) {
    def normalize = s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
  }

}

trait Jsonize

