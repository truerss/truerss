package truerss.system

import akka.actor.{ActorLogging, Actor}
import com.github.truerss.base.{BaseFeedReader, BaseContentReader}
import truerss.util.ApplicationPlugins

import scala.util._

class NetworkActor extends Actor with ActorLogging {

  import network._

  var feedMap: Map[Long, BaseFeedReader] = Map.empty
  var contentMap: Map[Long, BaseContentReader] = Map.empty

  def receive = {
    case NetworkInitialize(z) =>
      log.info("Network initialize")
      feedMap = z.map(x => x.sourceId -> x.feedReader).toMap
      contentMap = z.map(x => x.sourceId -> x.contentReader).toMap

    case Grep(sourceId, url) =>
      log.info(s"Extract feeds for ${url} sourceId = ${sourceId}")
      feedMap.get(sourceId) match {
        case Some(plugin) =>
          plugin.newEntries(url) match {
            case Right(xs) => sender ! ExtractedEntries(sourceId, xs)
            case Left(error) => sender ! ExtractError(error.error)
          }
        case None =>
          sender ! SourceNotFound(sourceId)
      }

    case ExtractContent(sourceId, feedId, url) =>
      log.info(s"Extract content for ${feedId} -> ${url}")
      contentMap.get(sourceId) match {
        case Some(plugin) => plugin.content(url) match {
          case Right(content) =>
            sender ! ExtractContentForEntry(sourceId, feedId, content)
          case Left(error) =>
            sender ! ExtractError(error.error)
        }
        case None => sender ! SourceNotFound(sourceId)
      }
  }

}
