package truerss.plugins

import scala.util.Either

trait BaseReader { self: BasePlugin =>

  /**
   * This method check url, and return `true` when url for this site
   *
   * @param url - url string
   * @return Boolean
   */
  def matchUrl(url: String): Boolean

  //TODO pass last N entries ?
  /**
   * Return list of last entries for given site
   * @param url
   * @return List[Entry]
   */
  def newEntries(url: String): Either[String, Vector[Entry]]

  /**
   * Extract content for given title
   *
   * @param url
   * @return
   */
  def content(url: String): Either[String, Option[String]]


  val priority = 0


}
