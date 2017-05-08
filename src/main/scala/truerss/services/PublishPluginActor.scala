package truerss.services

import akka.actor.{Actor, ActorLogging}
import com.github.truerss.base.{BasePublishPlugin, PublishActions}

import scala.collection.mutable.ArrayBuffer

import truerss.models.Feed
import truerss.util.Util



class PublishPluginActor(plugins: ArrayBuffer[BasePublishPlugin])
  extends Actor with ActorLogging {

  import PublishActions.Favorite
  import Util._
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
  case class PublishEvent(feed: Feed)
}