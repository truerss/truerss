package truerss.services.management

import akka.event.EventStream
import truerss.api.{BadRequestResponse, ImportResponse, Ok, Response}
import truerss.dto.NewSourceFromFileWithErrors
import truerss.services.actors.management.OpmlActor.{from, interval}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourcesService}

import scala.concurrent.{ExecutionContext, Future}

class OpmlManagement(opmlService: OpmlService,
                     sourcesService: SourcesService,
                     stream: EventStream
                    )
                    (implicit ec: ExecutionContext)
{

  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  type R = Future[Response]

  def getOpml: R = {
    opmlService.build.map(Ok)
  }

  def createFrom(opml: String): R = {
    Future {
      opmlService.parse(opml).fold(
        error => {
          logger.warn(s"Failed to parse given text as opml: $error")
          Future.successful(BadRequestResponse(error))
        },
        xs => {
          logger.info(s"Materialize ${xs.size} outlines from given file")
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
                logger.info(s"New sources was created: ${source.url}")
                stream.publish(SourcesKeeperActor.NewSource(source))
                index -> Right(source)
            }
          }

          Future.sequence(fs).map { tmp =>
            ImportResponse(tmp.toMap)
          }
        }
      )
    }.flatMap(identity)
  }

}
