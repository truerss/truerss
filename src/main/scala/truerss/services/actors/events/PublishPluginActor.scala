package truerss.services.actors.events

import java.time.ZoneOffset
import java.util.Date
import io.truerss.actorika._
import com.github.truerss.base.PublishActions.NewEntries
import com.github.truerss.base.{Entry, PublishActions}
import org.slf4j.LoggerFactory
import truerss.dto.FeedDto
import truerss.services.ApplicationPluginsService
import truerss.util.EnclosureImplicits.EnclosureDtoExt

class PublishPluginActor(appPluginService: ApplicationPluginsService)
  extends Actor {

  import PublishActions.Favorite
  import PublishPluginActor._

  private val logger = LoggerFactory.getLogger(getClass)

  def receive: Receive = {
    case AddToFavorites(feed) =>
      appPluginService.publishPlugins.foreach { pp =>
        logger.info(s"Publish to ${pp.pluginName}")
        pp.publish(Favorite(feed.toEntry))
      }

    case NewEntriesReceived(xs) =>
      appPluginService.publishPlugins.foreach(_.publish(NewEntries(xs.map(_.toEntry))))
  }

}

object PublishPluginActor {
  sealed trait PublishEvent
  case class AddToFavorites(feed: FeedDto) extends PublishEvent
  case class NewEntriesReceived(feeds: Iterable[FeedDto]) extends PublishEvent

  implicit class FeedDtoExt(val x: FeedDto) extends AnyVal {
    def toEntry: Entry = {
      Entry(
        url = x.url,
        title = x.title,
        author = x.author,
        publishedDate = new Date(x.publishedDate.toEpochSecond(ZoneOffset.UTC)),
        description = x.description,
        content = x.content,
        enclosure = x.enclosure.flatMap(_.toEnclosure)
      )
    }
  }

}