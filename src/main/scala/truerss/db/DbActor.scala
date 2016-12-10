package truerss.db

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import truerss.models.{CurrentDriver, FeedDao, Source, SourceDao}
import truerss.system
import truerss.system.util.Unread

import scala.concurrent.Future
import slick.jdbc.JdbcBackend.{DatabaseDef, SessionDef}

class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor with ActorLogging {

  import driver.query._
  import driver.profile.api._
  import system.db._
  import driver.DateSupport._
  import system.util.{FeedContentUpdate, SourceLastUpdate}
  import system.ws.NewFeeds
  import truerss.util.Util._


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

    // TODO move this logic in another place
    case AddFeeds(sourceId, xs) =>
      val urls = xs.map(_.url)
      val q = feeds.filter(_.sourceId === sourceId)
        .filter(_.url inSet urls)
      db.run {
        q.result
      }.flatMap { inDb =>
        val inDbMap = inDb.map(f => f.url -> f).toMap
        val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)
        db.run {
          forceUpdateXs.map { entry =>
            val feed = entry.toFeed(sourceId)
            inDbMap.get(feed.url) match {
              case Some(_) =>
                q.filter(_.url === feed.url)
                  .map(f => (f.title, f.author, f.publishedDate,
                    f.description, f.normalized, f.content, f.read))
                  .update((feed.title, feed.author, feed.publishedDate,
                    feed.description.orNull,
                    feed.normalized, feed.content.orNull, false))
              case None =>
                feeds += feed
            }
          }

          val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
          val inDbUrls = inDbMap.keySet
          val fromNetwork = updateXsMap.keySet
          val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
          feeds ++= newFeeds.toSeq

          val newUrls = forceUpdateXs.map(_.url) ++ newFeeds.map(_.url)
          feeds.filter(_.sourceId === sourceId)
            .filter(_.url inSet newUrls)
            .result
        }
      }.map(_.toVector).map(NewFeeds) pipeTo sender

    case FeedContentUpdate(feedId, content) =>
      db.run {
        feeds.filter(_.id === feedId).map(_.content).update(content)
      }

  }
}
