package truerss.system.actors

import akka.actor._
import akka.actor.FSM
import truerss.models.Source
import truerss.controllers.{BadRequestResponse, ModelResponse}
import truerss.system.{db, util, ws}
import truerss.util.{ApplicationPlugins, SourceValidator, Util}

class AddSourceFSM(override val dbRef: ActorRef,
                   sourcesRef: ActorRef,
                   appPlugins: ApplicationPlugins)
  extends CommonActor with FSM[AddSourceFSM.State, AddSourceFSM.Data]
    with AddOrUpdateFSMHelpers {

  import db._
  import ws.SourceAdded
  import util.NewSource
  import Util._
  import AddSourceFSM._

  startWith(Idle, Uninitiated)

  when(Idle) {
    case Event(AddSource(source), Uninitiated) =>
      originalSender = sender
      SourceValidator.validate(source) match {
        case Right(validSource) =>
          val state = appPlugins.getState(validSource.url)
          val newSource = validSource.copy(state = state)
          dbRef ! UrlIsUniq(validSource.url)
          stay using Context(newSource)

        case Left(errors) =>
          originalSender ! BadRequestResponse(errors.mkString(", "))
          stop
      }

    case Event(ResponseFeedCheck(count), context: Context) =>
      dbRef ! NameIsUniq(context.source.name)
      goto(Validate) using context.copy(urlCheckCount = count)
  }

  when(Validate) {
    case Event(ResponseFeedCheck(count), context: Context) =>
      (context.urlCheckCount, count) match {
        case (0, 0) =>
          dbRef ! AddSource(context.source)
          goto(Finish) using context
        case (0, _) =>
          originalSender ! BadRequestResponse(nameError(context.source))
          stop
        case (_, 0) =>
          originalSender ! BadRequestResponse(urlError(context.source))
          stop
        case (_, _) =>
          originalSender ! BadRequestResponse(s"${urlError(context.source)}, ${nameError(context.source)}")
          stop
      }
  }

  when(Finish) {
    case Event(ResponseSourceId(id), context: Context) =>
      val source = context.source.copy(id = Some(id))
        .recount(0)
        .withState(appPlugins.getState(context.source.url))
      stream.publish(SourceAdded(source))
      sourcesRef ! NewSource(source)
      originalSender ! ModelResponse(source)
      stop
  }

  initialize()

}

object AddSourceFSM {
  sealed trait State
  case object Idle extends State
  case object Validate extends State
  case object Finish extends State

  sealed trait Data
  case object Uninitiated extends Data
  case class Context(
                      source: Source,
                      urlCheckCount: Int = 0
                    ) extends Data

  def props(dbRef: ActorRef,
            sourcesRef: ActorRef,
            appPlugins: ApplicationPlugins) =
    Props(classOf[AddSourceFSM], dbRef, sourcesRef, appPlugins)
}
