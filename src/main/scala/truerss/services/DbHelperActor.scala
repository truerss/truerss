package truerss.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.WebSockerController
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.db.DbLayer
import truerss.db._

class DbHelperActor(dbLayer: DbLayer)
  extends Actor with ActorLogging {

  import DbHelperActor._
  import FeedSourceDtoModelImplicits._
  import context.dispatcher

  val stream: EventStream = context.system.eventStream

  def receive: Receive = {
    case SourceLastUpdate(sourceId) =>
      dbLayer.sourceDao.updateLastUpdateDate(sourceId)

    case SetState(sourceId, state) =>
      dbLayer.sourceDao.updateState(sourceId, state)

    case AddFeeds(sourceId, xs) =>
      dbLayer.feedDao.mergeFeeds(sourceId, xs)
        .map(_.toVector)
        .map(xs => xs.map(_.toDto))
        .map(WebSockerController.NewFeeds)
        .foreach(stream.publish)

    case FeedContentUpdate(feedId, content) =>
      dbLayer.feedDao.updateContent(feedId, content)

  }
}

object DbHelperActor {
  def props(dbLayer: DbLayer) = {
    Props(classOf[DbHelperActor], dbLayer)
  }

  sealed trait DbHelperActorMessage
  case class AddFeeds(sourceId: Long, xs: Vector[Entry]) extends DbHelperActorMessage
  case class SetState(sourceId: Long, state: SourceState) extends DbHelperActorMessage
  case class SourceLastUpdate(sourceId: Long) extends DbHelperActorMessage
  case class FeedContentUpdate(feedId: Long, content: String) extends DbHelperActorMessage

}