package truerss.system

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import truerss.plugins.BasePlugin

import scala.util._

class NetworkActor extends Actor with ActorLogging {

  import network._

  var pluginMap: Map[Long, BasePlugin] = Map.empty

  def receive = {
    case NetworkInitialize(z) =>
      log.info("Network initialize")
      pluginMap = z.map(x => x.sourceId -> x.plugin).toMap

    case Grep(sourceId, url) =>
      log.info(s"Extract feeds for ${url} sourceId = ${sourceId}")
      pluginMap.get(sourceId) match {
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
      pluginMap.get(sourceId) match {
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
