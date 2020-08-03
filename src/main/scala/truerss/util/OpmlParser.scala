package truerss.util

import zio.Task

import scala.xml._

case class Outline(title: String, link: String)

object OpmlParser {
  import syntax._
  import ext._

  val _outline = "outline"
  val _title = "title"
  val _xmlUrl = "xmlUrl"


  def parse(s: String): Task[Iterable[Outline]] = {
    Task(load(s))
  }

  private def present(attr: String)(implicit node: Node): Boolean = {
    val r = node.attribute(attr)
    r.isDefined && r.forall(_.nonEmpty)
  }

  private def get(attr: String)(implicit node: Node): String = {
    node.attribute(attr).map(_.text).head
  }

  private def load(s: String): Iterable[Outline] = {
    val x = XML.loadString(s)
    (x \\ _outline).filter { implicit outline =>
      present(_title) && present(_xmlUrl)
    }.map { implicit outline =>
      Outline(get(_title), get(_xmlUrl))
    }
  }

}
