package truerss.db

import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.models.{CurrentDriver, FeedDao, SourceDao}
import truerss.system
import truerss.system.util.Unread

class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor with ActorLogging {

  import system.db._
  import system.util.{FeedContentUpdate, SourceLastUpdate}
  import system.ws.NewFeeds


  implicit val ec = context.system.dispatchers.lookup("dispatchers.db-dispatcher")

  val stream = context.system.eventStream

  val sourceDao = new SourceDao(db)(ec, driver)
  val feedDao = new FeedDao(db)(ec, driver)


  def receive = {
    case GetAll | OnlySources =>
      sourceDao.all.map(_.toVector).map(ResponseSources) pipeTo sender

    case Unread(sourceId) =>
      feedDao.findUnread(sourceId)
        .map(_.toVector)
        .map(ResponseFeeds) pipeTo sender

    case FeedCount(read) =>
      feedDao.feedBySourceCount(read)
        .map(_.toVector)
        .map(ResponseFeedCount) pipeTo sender

    case FeedCountForSource(sourceId) =>
      feedDao.feedCountBySourceId(sourceId)
        .map(ResponseCount) pipeTo sender

    case GetSource(sourceId) =>
      sourceDao.findOne(sourceId).map(ResponseMaybeSource) pipeTo sender

    case DeleteSource(sourceId) =>
      sourceDao.findOne(sourceId).map { source =>
        sourceDao.delete(sourceId)
        feedDao.deleteFeedsBySource(sourceId)
        source
      }.map(ResponseMaybeSource) pipeTo sender

    case AddSource(source) =>
      sourceDao.insert(source)
        .map(ResponseSourceId) pipeTo sender

    case UpdateSource(_, source) =>
      sourceDao.updateSource(source)
        .map(_.toLong)
        .map(ResponseSourceId) pipeTo sender

    case MarkAll =>
      feedDao.markAll
        .map(_.toLong)
        .map(ResponseDone) pipeTo sender

    case Mark(sourceId) =>
      sourceDao.findOne(sourceId).map { source =>
        feedDao.markBySource(sourceId)
        source
      }.map(ResponseMaybeSource) pipeTo sender

    case Latest(count) =>
      feedDao.lastN(count)
        .map(_.toVector)
        .map(ResponseFeeds) pipeTo sender

    case ExtractFeedsForSource(sourceId, from, limit) =>
      feedDao.pageForSource(sourceId, from, limit)
        .map(_.toVector)
        .map(ResponseFeeds) pipeTo sender

    case Favorites =>
      feedDao.favorites
        .map(_.toVector)
        .map(ResponseFeeds) pipeTo sender

    case GetFeed(num) =>
      feedDao.findOne(num)
        .map(ResponseMaybeFeed) pipeTo sender

    case MarkFeed(feedId) =>
      feedDao.findOne(feedId).map { feed =>
        feedDao.modifyFav(feedId, true)
        feed.map(_.mark(true))
      }.map(ResponseMaybeFeed) pipeTo sender

    case UnmarkFeed(feedId) =>
      feedDao.findOne(feedId).map { feed =>
        feedDao.modifyFav(feedId, false)
        feed.map(_.mark(false))
      }.map(ResponseMaybeFeed) pipeTo sender

    case MarkAsReadFeed(feedId) =>
      feedDao.findOne(feedId).map { feed =>
        feedDao.modifyRead(feedId, true)
        feed.map(f => f.copy(read = true))
      }.map(ResponseMaybeFeed) pipeTo sender

    case MarkAsUnreadFeed(feedId) =>
      feedDao.findOne(feedId).map { feed =>
        feedDao.modifyRead(feedId, false)
        feed.map(f => f.copy(read = false))
      }.map(ResponseMaybeFeed) pipeTo sender

    case UrlIsUniq(url, id) =>
      sourceDao.findByUrl(url, id).map(ResponseFeedCheck) pipeTo sender

    case NameIsUniq(name, id) =>
      sourceDao.findByName(name, id).map(ResponseFeedCheck) pipeTo sender

    case SourceLastUpdate(sourceId) =>
      sourceDao.updateLastUpdateDate(sourceId)

    case SetState(sourceId, state) =>
      sourceDao.updateState(sourceId, state)

    case AddFeeds(sourceId, xs) =>
      feedDao.mergeFeeds(sourceId, xs)
        .map(_.toVector)
        .map(NewFeeds)
        .foreach(stream.publish)

    case FeedContentUpdate(feedId, content) =>
      feedDao.updateContent(feedId, content)

  }
}
