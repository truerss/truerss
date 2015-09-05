package truerss.system

import akka.actor.{Actor, ActorLogging}

import com.github.truerss.base.{PublishActions, BasePublishPlugin}

import scala.collection.mutable.ArrayBuffer

import truerss.util.Util


class PublishPluginActor(plugins: ArrayBuffer[BasePublishPlugin])
  extends Actor with ActorLogging {

  import util.PublishEvent
  import PublishActions.Favorite
  import Util._

  def receive = {
    case PublishEvent(feed) =>
      plugins.foreach { pp =>
        log.info(s"Publish to ${pp.pluginName}")
        pp.publish(Favorite, feed.toEntry)
      }
  }

}
