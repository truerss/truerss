package truerss.plugins

import scala.util.Either

trait BaseFeedReader {
  self: Priority with UrlMatcher with ConfigProvider =>

  import Errors.Error

  /**
   * Return list of last entries for given site
   * @param url
   * @return List[Entry]
   */
  def newEntries(url: String): Either[Error, Vector[Entry]]
}

