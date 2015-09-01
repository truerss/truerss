package truerss.plugins

import scala.util.Either

/**
 * Created by mike on 1.9.15.
 */
trait BaseContentReader {
  self: Priority with UrlMatcher with ConfigProvider =>

  import Errors.Error

  /**
   * Extract content for given title
   *
   * @param url
   * @return
   */
  def content(url: String): Either[Error, Option[String]]
}
