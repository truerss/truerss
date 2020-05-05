package truerss.plugins

import java.net.URL
import com.github.truerss.base.ContentTypeParam.{RequestParam, HtmlRequest, UrlRequest}
import com.github.truerss.base.{ContentTypeParam, Video, BaseContentPlugin, Errors}
import com.typesafe.config.{Config, ConfigFactory}


class YoutubePlugin(config: Config = ConfigFactory.empty)
  extends BaseContentPlugin(config) {

  override val pluginName = "YoutubePlugin"
  override val author = "<mike.fch1@gmail.com>"
  override val about = "Embededed Youtube Video"
  override val version = "0.0.1"

  override val contentType = Video
  override val priority = 10
  override val contentTypeParam = ContentTypeParam.URL

  private val links = Vector("youtube.com", "youtu.be", "y2u.be")

  override def matchUrl(url: URL): Boolean = {
    val host = url.getHost
    if (links.exists(x => host.endsWith(x))) {
      true
    } else {
      false
    }
  }

  override def content(urlOrContent: RequestParam): Response = {
    urlOrContent match {
      case UrlRequest(url) =>
        val need = url.toString.replace("watch?v=", "embed/")
        Right(Some(s"""
          <iframe width="560" height="315"
          src="$need"
          frameborder="0" allowfullscreen>
          </iframe>
        """))
      case HtmlRequest(_) =>
        Left(Errors.UnexpectedError("Pass url instead of content"))
    }
  }

}
