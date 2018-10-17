package truerss.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import truerss.db.DbLayer
import truerss.services.actors._
import truerss.util.{ApplicationPlugins, Util}

import scala.language.postfixOps

class ApiActor(appPlugins: ApplicationPlugins,
               sourcesRef: ActorRef,
               dbLayer: DbLayer)
  extends Actor with ActorLogging {

  import Util._

  import context.dispatcher // TODO

  val stream = context.system.eventStream

  val sourcesService = new SourcesService(dbLayer, appPlugins)
  val applicationPluginsService = new ApplicationPluginsService(appPlugins)
  val opmlService = new OpmlService(sourcesService)

  def create(props: Props) =
    context.actorOf(props.withDispatcher("dispatchers.truerss-dispatcher"))

  def receive = LoggingReceive {
    case msg: SourcesManagementActor.SourcesMessage =>
      create(SourcesManagementActor.props(sourcesService)) forward msg

    case msg: FeedsManagementActor.FeedsMessage =>
      create(FeedsManagementActor.props(dbLayer)) forward msg

    case msg: OpmlActor.OpmlActorMessage =>
      create(OpmlActor.props(opmlService)) forward msg

    case msg: AddSourcesActor.AddSourcesActorMessage =>
      create(AddSourcesActor.props(dbLayer)) forward msg

    case msg: PluginManagementActor.PluginManagementMessage =>
      create(PluginManagementActor.props(appPlugins)) forward msg

    case msg: SourceActor.ExtractContent =>
      sourcesRef forward msg

    case msg: SourcesActor.SourcesMessage =>
      stream.publish(msg)
      sender ! ResponseHelpers.ok
  }

  override def unhandled(m: Any) = log.warning(s"Unhandled $m")

}

object ApiActor {
  def props(appPlugins: ApplicationPlugins, sourcesRef: ActorRef, dbLayer: DbLayer) = {
    Props(classOf[ApiActor], appPlugins, sourcesRef, dbLayer)
  }
}