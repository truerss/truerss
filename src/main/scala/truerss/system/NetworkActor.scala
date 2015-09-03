package truerss.system

import akka.actor.{ActorLogging, Actor}
import com.github.truerss.base.{PluginInfo, BaseFeedReader, BaseContentReader}
import scala.collection.mutable.{Map => M}

import scala.util._

class NetworkActor extends Actor with ActorLogging {

  import network._
  import context._

  val feedMap: M[Long, BaseFeedReader] = M.empty
  val contentMap: M[Long, BaseContentReader] = M.empty


  def receive = {
    case NetworkInitialize(xs) =>
      log.info("Network initialize")
      xs.foreach { x =>
        log.info(s"====> ${x.sourceId} | ${x.feedReader} | ${x.contentReader}")
        feedMap += x.sourceId -> x.feedReader
        contentMap += x.sourceId -> x.contentReader
      }
      sender ! NetworkInitialized

    case NewSourceInfo(info) =>
      feedMap += info.sourceId -> info.feedReader
      contentMap += info.sourceId -> info.contentReader

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
