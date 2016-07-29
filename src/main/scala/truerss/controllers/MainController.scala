package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.{HttpCookie, MediaTypes}
import spray.routing.HttpService
import scala.io.Source

trait MainController extends BaseController with WsPortProvider with Redirectize {

  import java.net.URLEncoder
  import java.nio.charset.Charset
  val utf8 = Charset.forName("UTF-8").name()
  import HttpService._

  val fileName = "index.html"

  def root = {
    optionalHeaderValueByName(Redirect) { mbRedirect =>
      setCookie(HttpCookie("port", content = s"$wsPort"),
        HttpCookie(Redirect,
          content = URLEncoder.encode(mbRedirect.getOrElse("/"), utf8),
          path = Some("/")
        )
      ) {
        respondWithMediaType(MediaTypes.`text/html`) {
          complete(Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString)
        }
      }
    }
  }

  def about = {
    complete("""
      <h1>About</h1>
      <p>
        TrueRss is open source feed reader with customizable plugin system
        for any content (atom, rss, youtube channels...).
        More info <a href='http://truerss.net'>truerss official site</a>
        Download plugins: <a href='https://github.com/truerss?utf8=%E2%9C%93&query=plugin'>plugins</a>
      </p>
      <ul>
        <li><code>left-arrow</code> - next post</li>
        <li><code>right-arrow</code> - previous post</li>
        <li><code>shift+n</code> - next source</li>
        <li><code>shift+p</code> - previous source</li>
        <li><code>shift+f</code> - mark\\unmark as favorite</li>
        <li><code>shift+m</code> - mark as read</li>
      </ul>  

             """.stripMargin)
  }



}
