package truerss.db

import java.util.Date

import akka.actor.{ActorLogging, Actor}
import akka.event.LoggingReceive
import akka.pattern._
import scala.concurrent.Future
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import truerss.models.{CurrentDriver, Source, Feed}
import truerss.system
/**
 * Created by mike on 2.8.15.
 */
class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor with ActorLogging {

  import system.db._
  import system.util.{SourceLastUpdate, FeedContentUpdate}
  import driver.query._
  import driver.profile.simple._
  import context.dispatcher

  def receive = {
    case GetAll | OnlySources => Future.successful{ db withSession { implicit session =>
      sources.buildColl
    }} pipeTo sender

    case FeedCount(read) => Future.successful {
      db withSession { implicit session =>
        feeds.filter(_.read === read).groupBy(_.sourceId).map {
          case (sourceId, xs) => sourceId -> xs.size
        }.buildColl
      }
    } pipeTo sender

    case GetSource(sourceId) => Future.successful {
      db withSession { implicit session =>
        sources.filter(_.id === sourceId).firstOption
      }
    } pipeTo sender

    case DeleteSource(sourceId) => Future.successful {
      db withSession { implicit session =>
        val res = sources.filter(_.id === sourceId).firstOption
        sources.filter(_.id === sourceId).delete
        res
      }
    } pipeTo sender

    case AddSource(source) => Future.successful {
      db withSession { implicit session =>
        (sources returning sources.map(_.id)) += source
      }
    } pipeTo sender

    case UpdateSource(num, source) => Future.successful {
      db withSession { implicit session =>
        sources.filter(_.id === source.id)
          .map(s => (s.url, s.name, s.interval, s.plugin, s.normalized))
          .update(source.url, source.name, source.interval,
            source.plugin, source.normalized)
      }
    } pipeTo sender

    case MarkAll(sourceId) => Future.successful {
      db withSession { implicit session =>
        val source = sources.filter(_.id === sourceId).firstOption
        feeds.filter(_.sourceId === sourceId).map(f => f.read).update(true)
        source
      }
    } pipeTo sender

    case Latest(count) => Future.successful {
      db withSession { implicit session =>
        feeds.filter(_.read === false).take(count).sortBy(_.publishedDate).buildColl
      }
    }  pipeTo sender

    case ExtractFeedsForSource(sourceId) => Future.successful {
      db withSession { implicit session =>
        feeds.filter(_.sourceId === sourceId).buildColl
      }
    } pipeTo sender

    case Favorites => Future.successful {
      db withSession { implicit session =>
        feeds.filter(_.favorite === true).buildColl
      }
    } pipeTo sender

    case GetFeed(num) => Future.successful {
      db withSession { implicit session =>
        feeds.filter(_.id === num).firstOption
      }
    } pipeTo sender

    case MarkFeed(feedId) => Future.successful {
      db withSession { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(true)
        res
      }
    } pipeTo sender

    case UnmarkFeed(feedId) => Future.successful {
      db withSession { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.favorite).update(false)
        res
      }
    } pipeTo sender

    case MarkAsReadFeed(feedId) => Future.successful {
      db withSession { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.read).update(true)
        res
      }
    } pipeTo sender


    case MarkAsUnreadFeed(feedId) => Future.successful {
      db withSession { implicit session =>
        val res = feeds.filter(_.id === feedId).firstOption
        feeds.filter(_.id === feedId).map(e => e.read).update(false)
        res
      }
    } pipeTo sender

    case UrlIsUniq(url, id) =>
      Future.successful {
      db withSession { implicit session =>
        id.map(id => sources.filter(s => s.url === url && !(s.id === id)))
          .getOrElse(sources.filter(s => s.url === url)).length.run
      }
    } pipeTo sender

    case NameIsUniq(name, id) => Future.successful {
      db withSession { implicit session =>
        id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
        .getOrElse(sources.filter(s => s.name === name)).length.run
      }
    } pipeTo sender

    case SourceLastUpdate(sourceId) =>
      db withSession { implicit session =>
        sources.filter(_.id === sourceId)
          .map(s => s.lastUpdate).update(new Date())
      }

    case AddFeeds(sourceId, xs) =>
      db withSession { implicit session =>
        val alreadyInDbUrl = feeds.filter(_.sourceId === sourceId).map(_.url).run.toVector
        val fromNetwork = xs.map(_.url)
        val xsMap = xs.map(x => x.url -> x).toMap
        val newFeeds = (fromNetwork diff alreadyInDbUrl).flatMap { x =>
          xsMap.get(x)
        }
        log.info(s"for ${sourceId} feeds in db: ${alreadyInDbUrl.size}; " +
          s"from network ${fromNetwork.size}; new = ${newFeeds.size}")
        feeds.insertAll(newFeeds : _*)
      }

    case FeedContentUpdate(feedId, content) =>
      db withSession { implicit session =>
        feeds.filter(_.id === feedId).map(_.content).update(content)
      }

  }


}
