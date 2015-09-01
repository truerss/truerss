package truerss.plugins

/**
 * Created by mike on 1.9.15.
 */
trait UrlMatcher {
  def matchUrl(url: String): Boolean
}
