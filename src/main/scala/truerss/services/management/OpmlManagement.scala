package truerss.services.management

import truerss.api.{BadRequestResponse, ImportResponse, OpmlResponse}
import truerss.services.OpmlService

import scala.concurrent.ExecutionContext

class OpmlManagement(opmlService: OpmlService)
                    (implicit ec: ExecutionContext) extends BaseManagement {

  def getOpml: Z = {
    opmlService.build.map(OpmlResponse)
  }

  def createFrom(opml: String): Z = {
    opmlService.create(opml).fold(
      error => {
        logger.warn(s"Failed to process: $error")
        BadRequestResponse("Failed to convert")
      },
      ImportResponse
    )
  }

}
