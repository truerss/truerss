package truerss.system

import akka.actor.{Actor, ActorLogging}

import com.github.truerss.base.{BaseContentReader, BaseFeedReader}

import scala.collection.mutable.{Map => M}
import scala.util._

class NetworkActor extends Actor with ActorLogging {

  import network._

  var feedMap: M[Long, BaseFeedReader] = M.empty
  var contentMap: M[Long, BaseContentReader] = M.empty

  def receive = {
    case NetworkInitialize(f, c) =>
      log.info("Network initialize")
      feedMap = f
      contentMap = c
      sender ! NetworkInitialized

    case NewSourceInfo(id, f, c) =>
      feedMap += id -> f
      contentMap += id -> c

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
