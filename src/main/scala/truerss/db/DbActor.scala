package truerss.db

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.pattern._

import truerss.models.CurrentDriver
import truerss.system
import truerss.system.util.Unread

import scala.concurrent.Future
import scala.slick.jdbc.JdbcBackend.{DatabaseDef, SessionDef}

class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor with ActorLogging {

  import context.dispatcher
  import driver.profile.simple._
  import driver.query._
  import system.db._
  import system.util.{FeedContentUpdate, SourceLastUpdate}
  import system.ws.NewFeeds
  import truerss.util.Util._

  val stream = context.system.eventStream

  def complete[T] = (f: SessionDef => T) =>
    Future.successful(db withSession(f)) pipeTo sender

  def receive = {
    case GetAll | OnlySources =>
      complete { implicit session =>
        sources.buildColl
      }

    case Unread(sourceId) =>
      complete { implicit session =>
        feeds.filter(_.sourceId === sourceId)
          .filter(_.read === false).sortBy(_.publishedDate.desc).buildColl
      }

    case FeedCount(read) =>
      complete { implicit session =>
        feeds.filter(_.read === read).groupBy(_.sourceId).map {
          case (sourceId, xs) => sourceId -> xs.size
        }.buildColl
      }

    case FeedCountForSource(sourceId) =>
      complete { implicit session =>
        feeds.filter(_.sourceId === sourceId).length.run
      }

    case GetSource(sourceId) =>
      complete { implicit session =>
        sources.filter(_.id === sourceId).firstOption
      }

    case DeleteSource(sourceId) =>
      complete { implicit session =>
        val res = sources.filter(_.id === sourceId).firstOption
        sources.filter(_.id === sourceId).delete
        res
      }

    case AddSource(source) =>
      complete { implicit session =>
        (sources returning sources.map(_.id)) += source
      }

    case UpdateSource(num, source) =>
      complete { implicit session =>
        sources.filter(_.id === source.id)
          .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
          .update(source.url, source.name, source.interval,
            source.state, source.normalized).toLong
      }

    case MarkAll(sourceId) =>
      complete { implicit session =>
        feeds.filter(_.sourceId === sourceId).map(f => f.read).update(true)
        sources.filter(_.id === sourceId).firstOption
      }

    case Latest(count) =>
      complete { implicit session =>
        feeds.filter(_.read === false).take(count).sortBy(_.publishedDate.desc).buildColl
      }

    case ExtractFeedsForSource(sourceId, from, limit) =>
      complete { implicit session =>
        feeds.filter(_.sourceId === sourceId)
          .sortBy(_.publishedDate.desc).drop(from).take(limit).buildColl
      }

    case Favorites =>
      complete { implicit session =>
        feeds.filter(_.favorite === true).buildColl
      }

    case GetFeed(num) =>
      complete { implicit session =>
        feeds.filter(_.id === num).firstOption
      }

    case MarkFeed(feedId) =>
      complete { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(true)
        res
      }

    case UnmarkFeed(feedId) =>
      complete { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(false)
        res
      }

    case MarkAsReadFeed(feedId) =>
      complete { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.read).update(true)
        res
      }

    case MarkAsUnreadFeed(feedId) =>
      complete { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.read).update(false)
        res
      }

    case UrlIsUniq(url, id) =>
      complete { implicit session =>
        id.map(id => sources.filter(s => s.url === url && !(s.id === id)))
          .getOrElse(sources.filter(s => s.url === url)).length.run
      }

    case NameIsUniq(name, id) => complete { implicit session =>
        id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
        .getOrElse(sources.filter(s => s.name === name)).length.run
      }

    case SourceLastUpdate(sourceId) =>
      db withSession { implicit session =>
        sources.filter(_.id === sourceId)
          .map(s => s.lastUpdate).update(new Date())
      }

    case SetState(sourceId, state) =>
      db withSession { implicit session =>
        sources.filter(_.id === sourceId).map(s => s.state).update(state)
      }

    case AddFeeds(sourceId, xs) =>
      val newFeeds = db withSession { implicit session =>
        val urls = xs.map(_.url)
        val inDb = feeds.filter(_.sourceId === sourceId)
          .filter(_.url inSet(urls))
        val inDbMap = inDb.map(f => f.url -> f).toMap
        val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)
        forceUpdateXs.map { entry =>
          val feed = entry.toFeed(sourceId)
          inDbMap.get(feed.url) match {
            case Some(alreadyInDb) =>
              inDb.filter(_.url === feed.url)
                .map(f => (f.title, f.author, f.publishedDate,
                  f.description, f.normalized, f.content, f.read))
                  .update((feed.title, feed.author, feed.publishedDate,
                  feed.description.orNull,
                    feed.normalized, feed.content.orNull, false))
            case None =>
              feeds.insert(feed)
          }
        }
        val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
        val inDbUrls = inDbMap.keySet
        val fromNetwork = updateXsMap.keySet
        val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
        feeds.insertAll(newFeeds.toSeq : _*)

        val newUrls = forceUpdateXs.map(_.url) ++ newFeeds.map(_.url)
        feeds.filter(_.sourceId === sourceId)
          .filter(_.url inSet(newUrls)).buildColl
      }
      stream.publish(NewFeeds(newFeeds.toVector))

    case FeedContentUpdate(feedId, content) =>
      db withSession { implicit session =>
        feeds.filter(_.id === feedId).map(_.content).update(content)
      }

  }
}
