package truerss.system.actors

import akka.actor._
import truerss.api.OkResponse
import truerss.system.{db, util}

import scala.xml.Utility

class OpmlActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{GetAll, ResponseSources}
  import util.Opml

  override def defaultHandler: Receive = {
    case Opml =>
      originalSender = sender
      dbRef ! GetAll

    case ResponseSources(sources) =>
      val exportText = "Newsfeeds exported from Truerss"
      val outlines = sources.map { source =>
        s"""<outline type="rss" text="${e(source.name)}" title="${e(source.name)}" xmlUrl="${e(source.url)}"></outline>"""
      }.mkString("\n")

      originalSender ! OkResponse(
        s"""|<?xml version="1.0"?>
         |<opml version="1.0">
         |<head>TrueRSS Feed List Export</head>
         |<body>
         |<outline title="$exportText" text="$exportText" description="$exportText" type="folder">
         |$outlines
         |</outline>
         |</body>
         |</opml>
       """.stripMargin)
  }

  private def e(x: String) = Utility.escape(x)

}

object OpmlActor {
  def props(dbRef: ActorRef) =
    Props(classOf[OpmlActor], dbRef)
}