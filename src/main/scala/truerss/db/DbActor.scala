package truerss.db

import java.util.Date

import akka.actor.{Actor, ActorLogging}
import akka.pattern._

import truerss.models.CurrentDriver
import truerss.system

import scala.concurrent.Future
import scala.slick.jdbc.JdbcBackend.{DatabaseDef, SessionDef}
import scalaz.Scalaz._
import scalaz._
/**
 * Created by mike on 2.8.15.
 */
class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor with ActorLogging {

  import context.dispatcher
  import driver.profile.simple._
  import driver.query._
  import system.db._
  import system.util.{FeedContentUpdate, SourceLastUpdate}
  import system.ws.NewFeeds

  val stream = context.system.eventStream

  def complete[T] = (f: SessionDef => T) => Future.successful(db withSession(f)) pipeTo sender

  def receive = {
    case GetAll | OnlySources =>
      complete { implicit session =>
        sources.buildColl
      }

    case FeedCount(read) =>
      complete { implicit session =>
        feeds.filter(_.read === read).groupBy(_.sourceId).map {
          case (sourceId, xs) => sourceId -> xs.size
        }.buildColl
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
        val source = sources.filter(_.id === sourceId).firstOption
        feeds.filter(_.sourceId === sourceId).map(f => f.read).update(true)
        source
      }

    case Latest(count) =>
      complete { implicit session =>
        feeds.filter(_.read === false).take(count).sortBy(_.publishedDate).buildColl
      }

    case ExtractFeedsForSource(sourceId) =>
      complete { implicit session =>
        feeds.filter(_.sourceId === sourceId).buildColl
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

    case AddFeeds(sourceId, xs) =>
      val newFeeds = db withSession { implicit session =>
        val alreadyInDbUrl = feeds.filter(_.sourceId === sourceId).map(_.url).run.toVector
        val fromNetwork = xs.map(_.url)
        val xsMap = xs.map(x => x.url -> x).toMap
        val newFeeds = (fromNetwork diff alreadyInDbUrl).flatMap { x =>
          xsMap.get(x)
        }
        log.info(s"for ${sourceId} feeds in db: ${alreadyInDbUrl.size}; " +
          s"from network ${fromNetwork.size}; new = ${newFeeds.size}")
        val result = (feeds returning feeds.map(_.id)) ++= newFeeds
        result.zip(newFeeds).map { case p @ (id, feed) =>
          feed.copy(id = id.some)
        }.toVector
      }
      stream.publish(NewFeeds(newFeeds))


    case FeedContentUpdate(feedId, content) =>
      db withSession { implicit session =>
        feeds.filter(_.id === feedId).map(_.content).update(content)
      }

  }


}
