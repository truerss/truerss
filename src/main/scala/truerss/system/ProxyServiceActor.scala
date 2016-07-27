package truerss.system

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import truerss.controllers.BadRequestResponse
import truerss.system.actors._
import truerss.util.{ApplicationPlugins, Jsonize, SourceValidator, Util}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ProxyServiceActor(appPlugins: ApplicationPlugins,
                        dbRef: ActorRef,
                        sourcesRef: ActorRef, parent: ActorRef)
  extends Actor with ActorLogging {

  import Util._
  import context.dispatcher
  import db._
  import global._
  import network._
  import plugins.GetPluginList
  import responseHelpers._
  import truerss.controllers.{
    InternalServerErrorResponse,
    ModelResponse,
    ModelsResponse,
    OkResponse
  }
  import truerss.models.{Feed, Source}
  import util._
  import ws._

  implicit val timeout = Timeout(7 seconds)

  val stream = context.system.eventStream

  val publishActor = context.actorOf(Props(
    classOf[PublishPluginActor], appPlugins.publishPlugin),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishEvent])
  stream.subscribe(dbRef, classOf[SourceLastUpdate])
  stream.subscribe(dbRef, classOf[FeedContentUpdate])
  stream.subscribe(dbRef, classOf[AddFeeds])
  stream.subscribe(dbRef, classOf[SetState])

  def create(props: Props) = context.actorOf(props)


  def addOrUpdate[T <: Jsonize](msg: Sourcing,
                                f: Long => ModelResponse[T]) = {
    SourceValidator.validate(msg.source) match {
      case Right(source) =>
        val state = appPlugins.getState(msg.source.url)
        val newSource = msg.source.copy(state = state)
        val (newMsg, checkUrl, checkName) = msg match {
          case AddSource(_) => (AddSource(newSource), UrlIsUniq(msg.source.url),
                NameIsUniq(msg.source.name))
          case UpdateSource(sId, _) =>
            (UpdateSource(sId, newSource), UrlIsUniq(msg.source.url, msg.source.id),
              NameIsUniq(msg.source.name, msg.source.id))
        }

        (for {
          urlIsUniq <- (dbRef ? checkUrl).mapTo[Int]
          nameIsUniq <- (dbRef ? checkName).mapTo[Int]
        } yield {
            val tofb = (x: String) => Future.successful(BadRequestResponse(x))
            val u = s"Url '${newSource.url}' already present in db"
            val n = s"Name '${newSource.name}' not unique"
            (urlIsUniq, nameIsUniq) match {
              case (0, 0) =>
                (dbRef ? newMsg).mapTo[Long].map(f)
              case (0, _) => tofb(n)
              case (_, 0) => tofb(u)
              case (_, _) => tofb(s"$u, $n")
            }
          }).flatMap(identity)

      case Left(errs) => Future.successful(
        BadRequestResponse(errs.mkString(", ")))
    }
  }

  def dbReceive: Receive = {
    case OnlySources => dbRef forward OnlySources

    case GetAll =>
      create(GetAllActor.props(dbRef)) forward GetAll

    case msg: Unread =>
      create(UnreadActor.props(dbRef)) forward msg

    case msg: DeleteSource =>
      create(DeleteSourceActor.props(dbRef, sourcesRef)) forward msg

    case msg : Numerable =>
      create(NumerableActor.props(dbRef)) forward msg

    case msg: AddSource =>
      addOrUpdate(
        msg,
        (x: Long) => {
          val source = msg.source.copy(id = Some(x))
          val newSource = source.recount(0).withState(appPlugins.getState(source.url))
          stream.publish(SourceAdded(newSource))
          sourcesRef ! NewSource(newSource)
          ModelResponse(newSource)
        }
      ) pipeTo sender

    case msg: UpdateSource =>
      addOrUpdate(
        msg,
        (x: Long) => {
          val source = msg.source.copy(id = Some(x))
          val frontendSource = source.recount(0)
          stream.publish(SourceUpdated(frontendSource))
          //TODO update source actor
          ModelResponse(frontendSource)
        }
      ) pipeTo sender

    case msg: ExtractFeedsForSource =>
      create(FetchFeedsForSourceActor.props(dbRef)) forward msg

    case msg @ (_: Latest | _ : Favorites.type) =>
      create(LatestFavoritesActor.props(dbRef)) forward msg

    case MarkAll =>
      create(MarkAllActor.props(dbRef)) forward MarkAll

    // also necessary extract content if need
    case msg: GetFeed =>
      create(GetFeedActor.props(dbRef, sourcesRef)) forward msg

    case msg : MarkFeed =>
      create(MarkFeedActor.props(dbRef)) forward msg

    case msg @ (_ : UnmarkFeed |
                _ : MarkAsReadFeed | _ : MarkAsUnreadFeed)  =>
      create(MarkMessagesActor.props(dbRef)) forward msg

    case msg: SetState =>
      stream.publish(msg)

  }

  def networkReceive: Receive = {
    case msg: NewFeeds => stream.publish(msg)
  }

  def utilReceive: Receive = {
    case msg: Notify => stream.publish(msg)
    case msg @ ( _ : Update.type | _ : UpdateOne) =>
      sourcesRef forward msg
      sender ! ok
  }

  def pluginReceive: Receive = {
    case GetPluginList => sender ! ModelResponse(appPlugins)
  }

  def systemReceive: Receive = {
    case RestartSystem =>
      sourcesRef ! RestartSystem
      sender ! ok

    case StopSystem =>
      parent ! StopSystem
      sender ! ok

    case StopApp =>
      parent ! StopApp
      sender ! ok
  }

  def receive = dbReceive orElse
    networkReceive orElse
    utilReceive orElse
    pluginReceive orElse
    systemReceive

  override def unhandled(m: Any) = log.warning(s"Unhandled $m")

}
