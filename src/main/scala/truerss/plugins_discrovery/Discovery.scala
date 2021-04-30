package truerss.plugins_discrovery

sealed trait Discovery {
  def url: String
  def isValidSource(url: String): Boolean

}

case object GithubPluginDiscovery extends Discovery {
  override val url: String = "https://github.com"

  override def isValidSource(url: String): Boolean = {
    url match {
      case s"https://github.com/$_/$_/releases/tag/$_" => true
      case _ => false
    }
  }
  // todo
  // unroll from github.com/org/repo => github.com/org/repo/releases/tag/latest
}
