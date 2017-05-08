package truerss.services.actors

import akka.actor._
import akka.pattern.pipe
import truerss.api.{BadRequestResponse, Ok}
import truerss.db.DbLayer
import truerss.models.{Notify, Source, SourceHelper}
import truerss.util.OpmlParser

import scala.concurrent.Future
import scala.xml.Utility

class OpmlActor(dbLayer: DbLayer) extends CommonActor {

  import OpmlActor._
  import context.dispatcher

  override def defaultHandler: Receive = {
    case GetOpml =>
      val result = dbLayer.sourceDao.all.map { sources =>
        makeResponse(sources)
      }

      result pipeTo sender

    case CreateOpmlFromFile(text) =>
      val result = Future {
        OpmlParser.parse(text).fold(
          error => {
            log.warning(s"Failed to parse given text as opml: $error")
            stream.publish(Notify.danger(s"Error when import file $error"))
            BadRequestResponse(error)
          },
          xs => {
            log.info(s"Materialize ${xs.size} outlines from given file")
            val result = xs.map { x =>
              SourceHelper.from(x.link, x.title, interval)
            }
            stream.publish(AddSourcesActor.AddSources(result))
            Ok("I'll try")
          }
        )
      }

      result pipeTo sender
  }

}

object OpmlActor {

  // util variables
  private val exportText = "Newsfeeds exported from Truerss"

  val interval = 8

  def props(dbLayer: DbLayer) =
    Props(classOf[OpmlActor], dbLayer)

  def makeResponse(sources: Seq[Source]) = {
    val outlines = sources.map { source =>
      s"""<outline type="rss" text="${e(source.name)}" title="${e(source.name)}" xmlUrl="${e(source.url)}"></outline>"""
    }.mkString("\n")

    Ok(
      s"""|<?xml version="1.0"?>
        |<opml version="1.0">
        |<head>TrueRSS Feed List Export</head>
        |<body>
        |<outline title="$exportText" text="$exportText" description="$exportText
" type="folder">
        |$outlines
        |</outline>
        |</body>
        |</opml>
       """.stripMargin)
    }

  private def e(x: String) = Utility.escape(x)

  sealed trait OpmlActorMessage
  case object GetOpml extends OpmlActorMessage
  case class CreateOpmlFromFile(text: String) extends OpmlActorMessage

}