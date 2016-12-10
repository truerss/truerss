package truerss.db

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.pattern._

import truerss.models.CurrentDriver
import truerss.system
import truerss.models.Source
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

  val stream = context.system.eventStream

  implicit val ec = context.system.dispatchers.lookup("dispatchers.db-dispatcher")

  def receive = {
    case GetAll | OnlySources =>
      db.run(sources.result).map(_.toVector).map(ResponseSources) pipeTo sender

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
      db.run {
        sources.filter(_.id === sourceId).take(1).result
      }.map(_.headOption).map(ResponseMaybeSource) pipeTo sender


    case DeleteSource(sourceId) =>
      db.run {
        val res = sources.filter(_.id === sourceId).take(1).result
        sources.filter(_.id === sourceId).delete
        feeds.filter(_.sourceId === sourceId).delete
        res
      }.map(_.headOption).map(ResponseMaybeSource) pipeTo sender

    case AddSource(source) =>
      db.run {
        (sources returning sources.map(_.id)) += source
      }.map(ResponseSourceId) pipeTo sender

    case UpdateSource(num, source) =>
//      db.run {
//        sources.filter(_.id === source.id)
//          .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
//          .update(source.url, source.name, source.interval,
//            source.state, source.normalized)
//      }.map(ResponseSourceId) pipeTo sender
//
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
//
//    case GetFeed(num) =>
//      complete { implicit session =>
//        ResponseMaybeFeed(
//          feeds.filter(_.id === num).firstOption
//        )
//      }
//
//    case MarkFeed(feedId) =>
//      complete { implicit session =>
//        val res = feeds.filter(_.id === feedId).firstOption
//        feeds.filter(_.id === feedId).map(e => e.favorite).update(true)
//        ResponseMaybeFeed(res.map(f => f.mark(true)))
//      }
//
//    case UnmarkFeed(feedId) =>
//      complete { implicit session =>
//        val res = feeds.filter(_.id === feedId).firstOption
//        feeds.filter(_.id === feedId).map(e => e.favorite).update(false)
//        ResponseMaybeFeed(res.map(f => f.mark(false)))
//      }
//
//    case MarkAsReadFeed(feedId) =>
//      complete { implicit session =>
//        val res = feeds.filter(_.id === feedId).firstOption
//        feeds.filter(_.id === feedId).map(e => e.read).update(true)
//        ResponseMaybeFeed(res.map(f => f.mark(true)))
//      }
//
//    case MarkAsUnreadFeed(feedId) =>
//      complete { implicit session =>
//        val res = feeds.filter(_.id === feedId).firstOption
//        feeds.filter(_.id === feedId).map(e => e.read).update(false)
//        ResponseMaybeFeed(res.map(f => f.mark(false)))
//      }
//
//    case UrlIsUniq(url, id) =>
//      complete { implicit session =>
//        ResponseFeedCheck(
//          id
//            .map(id => sources.filter(s => s.url === url && !(s.id === id)))
//            .getOrElse(sources.filter(s => s.url === url))
//            .length
//            .run
//        )
//      }
//
//    case NameIsUniq(name, id) => complete { implicit session =>
//        ResponseFeedCheck(
//          id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
//          .getOrElse(sources.filter(s => s.name === name))
//            .length.run
//        )
//      }
//
//    case SourceLastUpdate(sourceId) =>
//      db withSession { implicit session =>
//        sources.filter(_.id === sourceId)
//          .map(s => s.lastUpdate).update(new Date())
//      }
//
//    case SetState(sourceId, state) =>
//      db withSession { implicit session =>
//        sources.filter(_.id === sourceId).map(s => s.state).update(state)
//      }
//
//    case AddFeeds(sourceId, xs) =>
//      val newFeeds = db withSession { implicit session =>
//        val urls = xs.map(_.url)
//        val inDb = feeds.filter(_.sourceId === sourceId)
//          .filter(_.url inSet(urls))
//        val inDbMap = inDb.map(f => f.url -> f).toMap
//        val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)
//        forceUpdateXs.map { entry =>
//          val feed = entry.toFeed(sourceId)
//          inDbMap.get(feed.url) match {
//            case Some(alreadyInDb) =>
//              inDb.filter(_.url === feed.url)
//                .map(f => (f.title, f.author, f.publishedDate,
//                  f.description, f.normalized, f.content, f.read))
//                  .update((feed.title, feed.author, feed.publishedDate,
//                  feed.description.orNull,
//                    feed.normalized, feed.content.orNull, false))
//            case None =>
//              feeds.insert(feed)
//          }
//        }
//        val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
//        val inDbUrls = inDbMap.keySet
//        val fromNetwork = updateXsMap.keySet
//        val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
//        feeds.insertAll(newFeeds.toSeq : _*)
//
//        val newUrls = forceUpdateXs.map(_.url) ++ newFeeds.map(_.url)
//        feeds.filter(_.sourceId === sourceId)
//          .filter(_.url inSet(newUrls)).buildColl
//      }
//      stream.publish(NewFeeds(newFeeds.toVector))
//
//    case FeedContentUpdate(feedId, content) =>
//      db withSession { implicit session =>
//        feeds.filter(_.id === feedId).map(_.content).update(content)
//      }

  }
}
