package truerss.services

import akka.actor.{Actor, ActorLogging}
import com.github.truerss.base.{BasePublishPlugin, Entry, PublishActions}
import truerss.dto.FeedDto

import scala.collection.mutable.ArrayBuffer



class PublishPluginActor(plugins: ArrayBuffer[BasePublishPlugin])
  extends Actor with ActorLogging {

  import PublishActions.Favorite
  import PublishPluginActor._

  def receive = {
    case PublishEvent(feed) =>
      plugins.foreach { pp =>
        log.info(s"Publish to ${pp.pluginName}")
        pp.publish(Favorite, feed.toEntry)
      }
  }

}

object PublishPluginActor {
  case class PublishEvent(feed: FeedDto)

  implicit class FeedDtoExt(val x: FeedDto) extends AnyVal {
    def toEntry: Entry = {
      Entry(
        url = x.url,
        title = x.title,
        author = x.author,
        publishedDate = x.publishedDate,
        description = x.description,
        content = x.content
      )
    }
  }

}