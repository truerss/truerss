package truerss.services.actors.management

import akka.actor._
import akka.pattern.pipe
import truerss.api.{BadRequestResponse, Ok}
import truerss.dto.{NewSourceDto, Notify}
import truerss.services.OpmlService

import scala.concurrent.Future

class OpmlActor(opmlService: OpmlService) extends CommonActor {

  import OpmlActor._
  import context.dispatcher

  override def receive: Receive = {
    case GetOpml =>
      opmlService.build.map(Ok) pipeTo sender

    case CreateOpmlFromFile(text) =>
      val result = Future {
        opmlService.parse(text).fold(
          error => {
            log.warning(s"Failed to parse given text as opml: $error")
            // is it need?
            stream.publish(Notify.danger(s"Error when import file $error"))
            BadRequestResponse(error)
          },
          xs => {
            log.info(s"Materialize ${xs.size} outlines from given file")
            val result = xs.map { x =>
              from(x.link, x.title, interval)
            }
            stream.publish(SourcesManagementActor.AddSources(result))
            Ok("I'll try")
          }
        )
      } pipeTo sender
  }

}

object OpmlActor {

  val interval = 8

  def props(opmlService: OpmlService): Props = {
    Props(classOf[OpmlActor], opmlService)
  }

  sealed trait OpmlActorMessage
  case object GetOpml extends OpmlActorMessage
  case class CreateOpmlFromFile(text: String) extends OpmlActorMessage

  def from(url: String, name: String, interval: Int): NewSourceDto = {
    NewSourceDto(
      url = url,
      name = name,
      interval = interval
    )
  }

}