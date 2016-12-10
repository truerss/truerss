package truerss.db

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.pattern._
import truerss.models.{CurrentDriver, Source, SourceDao}
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


  def receive = {
    case GetAll | OnlySources =>
      sourceDao.all.map(_.toVector).map(ResponseSources) pipeTo sender

    case Unread(sourceId) =>
      db.run {
        feeds.filter(_.sourceId === sourceId)
          .filter(_.read === false)
          .sortBy(_.publishedDate.desc)
          .result
      }.map(_.toVector).map(ResponseFeeds) pipeTo sender

    case FeedCount(read) =>
      db.run {
        feeds
          .filter(_.read === read)
          .groupBy(_.sourceId)
          .map {
            case (sourceId, xs) => sourceId -> xs.size
          }.result
      }.map(_.toVector).map(ResponseFeedCount) pipeTo sender

    case FeedCountForSource(sourceId) =>
      db.run {
        feeds
        .filter(_.sourceId === sourceId)
        .length.result
      }.map(ResponseCount) pipeTo sender

    case GetSource(sourceId) =>
      sourceDao.findOne(sourceId).map(ResponseMaybeSource) pipeTo sender


    case DeleteSource(sourceId) =>
      sourceDao.findOne(sourceId).map { source =>
        sourceDao.delete(sourceId)
        //feeds.filter(_.sourceId === sourceId).delete
        source
      }.map(ResponseMaybeSource) pipeTo sender

    case AddSource(source) =>
      sourceDao.insert(source).map(ResponseSourceId) pipeTo sender

    case UpdateSource(_, source) =>
      sourceDao.updateSource(source).map(_.toLong).map(ResponseSourceId) pipeTo sender

    case MarkAll =>
      db.run {
        feeds
          .filter(_.read === false)
          .map(_.read)
          .update(true)
      }.map(_.toLong).map(ResponseDone) pipeTo sender


    case Mark(sourceId) =>
      db.run {
        feeds.filter(_.sourceId === sourceId)
          .map(f => f.read).update(true)
        sources.filter(_.id === sourceId).take(1).result
      }.map(_.headOption).map(ResponseMaybeSource)

    case Latest(count) =>
      db.run {
        feeds
          .filter(_.read === false)
          .take(count)
          .sortBy(_.publishedDate.desc)
          .result
      }.map(_.toVector).map(ResponseFeeds) pipeTo sender


    case ExtractFeedsForSource(sourceId, from, limit) =>
      db.run {
        feeds
          .filter(_.sourceId === sourceId)
          .sortBy(_.publishedDate.desc)
          .drop(from)
          .take(limit)
          .result
      }.map(_.toVector).map(ResponseFeeds) pipeTo sender

    case Favorites =>
      db.run {
        feeds
          .filter(_.favorite === true)
          .result
      }.map(_.toVector).map(ResponseFeeds) pipeTo sender

    case GetFeed(num) =>
      db.run {
        feeds.filter(_.id === num).take(1).result
      }.map(_.headOption).map(ResponseMaybeFeed) pipeTo sender

    case MarkFeed(feedId) =>
      db.run {
        val res = feeds.filter(_.id === feedId).take(1).result.headOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(true)
        res.map(f => f.map(_.mark(true)))
      }.map(ResponseMaybeFeed) pipeTo sender

    case UnmarkFeed(feedId) =>
      db.run {
        val res = feeds.filter(_.id === feedId).take(1).result.headOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(false)
        res.map(f => f.map(_.mark(false)))
      }.map(ResponseMaybeFeed) pipeTo sender

    case MarkAsReadFeed(feedId) =>
      db.run {
        val res = feeds.filter(_.id === feedId).take(1).result.headOption
        feeds.filter(_.id === feedId).map(e => e.read).update(true)
        res.map(f => f.map(_.mark(true)))
      }.map(ResponseMaybeFeed) pipeTo sender

    case MarkAsUnreadFeed(feedId) =>
      db.run {
        val res = feeds.filter(_.id === feedId).take(1).result.headOption
        feeds.filter(_.id === feedId).map(e => e.read).update(false)
        res.map(f => f.map(_.mark(false)))
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
