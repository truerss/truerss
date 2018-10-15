package truerss.services.actors

import akka.actor.Props
import truerss.api.{BadRequestResponse, ModelResponse}
import truerss.db.DbLayer
import truerss.dto.NewSourceDto
import truerss.models.{Notify, Source}

import scala.concurrent.duration._
/**
  * Created by mike on 7.5.17.
  */
class AddSourcesActor(val dbLayer: DbLayer) extends CommonActor {

  import AddSourcesActor._

  override def stopInterval: FiniteDuration = 3 minutes

  val service = context.parent

  override def defaultHandler: Receive = {
    case AddSources(xs) =>
      xs.foreach { x =>
        service ! SourcesManagementActor.AddSource(x)
      }

    case ModelResponse(source: Source) =>
      log.info(s"New sources was created: ${source.url}")

    case BadRequestResponse(error) =>
      stream.publish(Notify.danger(error))

    case any =>
      log.warning(s"Unexpected error, when create new source: $any")

  }
}

object AddSourcesActor {
  def props(dbLayer: DbLayer) = {
    Props(classOf[AddSourcesActor], dbLayer)
  }

  sealed trait AddSourcesActorMessage
  case class AddSources(xs: Iterable[NewSourceDto]) extends AddSourcesActorMessage

}
