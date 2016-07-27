package truerss.system

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import truerss.system.actors._
import truerss.util.{ApplicationPlugins, Util}

import scala.language.postfixOps

class ProxyServiceActor(appPlugins: ApplicationPlugins,
                        dbRef: ActorRef,
                        sourcesRef: ActorRef, parent: ActorRef)
  extends Actor with ActorLogging {

  import Util._
  import db._
  import global._
  import plugins.GetPluginList
  import ResponseHelpers._
  import truerss.controllers.ModelResponse
  import util._
  import ws._

  val stream = context.system.eventStream

  val publishActor = context.actorOf(Props(
    classOf[PublishPluginActor], appPlugins.publishPlugin),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishEvent])
  stream.subscribe(dbRef, classOf[SourceLastUpdate])
  stream.subscribe(dbRef, classOf[FeedContentUpdate])
  stream.subscribe(dbRef, classOf[AddFeeds])
  stream.subscribe(dbRef, classOf[SetState])

  def create(props: Props) =
    context.actorOf(props.withDispatcher("dispatchers.truerss-dispatcher"))

  def dbReceive: Receive = {
    case OnlySources =>
      dbRef forward OnlySources

    case GetAll =>
      create(GetAllActor.props(dbRef)) forward GetAll

    case msg: Unread =>
      create(UnreadActor.props(dbRef)) forward msg

    case msg: DeleteSource =>
      create(DeleteSourceActor.props(dbRef, sourcesRef)) forward msg

    case msg : Numerable =>
      create(NumerableActor.props(dbRef)) forward msg

    case msg: AddSource =>
      create(AddSourceFSM.props(dbRef, sourcesRef, appPlugins)) forward msg

    case msg: UpdateSource =>
      create(UpdateSourceFSM.props(dbRef, sourcesRef, appPlugins)) forward msg

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
