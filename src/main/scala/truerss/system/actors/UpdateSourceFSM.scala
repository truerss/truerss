package truerss.system.actors

import akka.actor.{FSM, _}
import truerss.controllers.{BadRequestResponse, ModelResponse}
import truerss.models.Source
import truerss.system.{db, ws, util}
import truerss.util.{ApplicationPlugins, SourceValidator, Util}

class UpdateSourceFSM(override val dbRef: ActorRef,
                      sourcesRef: ActorRef,
                      appPlugins: ApplicationPlugins)
  extends CommonActor with FSM[UpdateSourceFSM.State, UpdateSourceFSM.Data]
    with AddOrUpdateFSMHelpers {

  import UpdateSourceFSM._
  import Util._
  import db._
  import util.ReloadSource
  import ws.SourceUpdated

  startWith(Idle, Uninitiated)

  when(Idle) {
    case Event(UpdateSource(id, source), _) =>
      originalSender = sender
      SourceValidator.validate(source) match {
        case Right(validSource) =>
          val state = appPlugins.getState(validSource.url)
          val newSource = validSource.copy(state = state, id = Some(id))
          dbRef ! UrlIsUniq(validSource.url, Some(id))
          stay using Context(source = newSource, sourceId = id)

        case Left(errors) =>
          originalSender ! BadRequestResponse(errors.mkString(", "))
          stop
      }

    case Event(ResponseFeedCheck(count), context: Context) =>
      dbRef ! NameIsUniq(context.source.name, context.source.id)
      goto(Validate) using context.copy(urlCheckCount = count)
  }

  when(Validate) {
    case Event(ResponseFeedCheck(count), context: Context) =>
      (context.urlCheckCount, count) match {
        case (0, 0) =>
          dbRef ! UpdateSource(context.sourceId, context.source)
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
      val source = context.source
        .copy(id = Some(id))
        .recount(0)

      stream.publish(SourceUpdated(source))
      sourcesRef ! ReloadSource(source)
      originalSender ! ModelResponse(source)
      stop
  }

  initialize()

  override def defaultHandler: Receive = PartialFunction.empty

}

object UpdateSourceFSM {
  sealed trait State
  case object Idle extends State
  case object Validate extends State
  case object Finish extends State

  sealed trait Data
  case object Uninitiated extends Data
  case class Context(
                      source: Source,
                      urlCheckCount: Int = 0,
                      sourceId: Long = 0
                    ) extends Data


  def props(dbRef: ActorRef,
    sourcesRef: ActorRef,
    appPlugins: ApplicationPlugins) =
      Props(classOf[UpdateSourceFSM], dbRef, sourcesRef, appPlugins)
}