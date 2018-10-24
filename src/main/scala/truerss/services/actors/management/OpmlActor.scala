package truerss.services.actors.management

import akka.actor._
import akka.pattern.pipe
import truerss.api.{BadRequestResponse, ImportResponse, Ok}
import truerss.dto.{NewSourceDto, NewSourceFromFileWithErrors}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourcesService}

import scala.concurrent.Future

class OpmlActor(opmlService: OpmlService, sourcesService: SourcesService) extends CommonActor {

  import OpmlActor._
  import context.dispatcher

  override def receive: Receive = {
    case GetOpml =>
      opmlService.build.map(Ok) pipeTo sender

    case CreateOpmlFromFile(text) =>
      Future {
        opmlService.parse(text).fold(
          error => {
            log.warning(s"Failed to parse given text as opml: $error")
            Future.successful(BadRequestResponse(error))
          },
          xs => {
            log.info(s"Materialize ${xs.size} outlines from given file")
            val fs = xs.map { x =>
              from(x.link, x.title, interval)
            }.zipWithIndex.map { case (x, index) =>
              sourcesService.addSource(x).map {
                case Left(errors) =>
                  index -> Left(
                    NewSourceFromFileWithErrors(
                      url = x.url,
                      name = x.name,
                      errors = errors
                    )
                  )

                case Right(source) =>
                  log.info(s"New sources was created: ${source.url}")
                  stream.publish(SourcesKeeperActor.NewSource(source))
                  index -> Right(source)
              }
            }

            Future.sequence(fs).map { tmp =>
              ImportResponse(tmp.toMap)
            }
          }
        )
      }.flatMap(identity) pipeTo sender
  }

}

object OpmlActor {

  val interval = 8

  def props(opmlService: OpmlService, sourcesService: SourcesService): Props = {
    Props(classOf[OpmlActor], opmlService, sourcesService)
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