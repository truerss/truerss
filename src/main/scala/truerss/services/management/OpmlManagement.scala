package truerss.services.management

import truerss.api.{BadRequestResponse, ImportResponse, OpmlResponse}
import truerss.services.OpmlService

import scala.concurrent.ExecutionContext

class OpmlManagement(opmlService: OpmlService)
                    (implicit ec: ExecutionContext) extends BaseManagement {

  def getOpml: R = {
    opmlService.build.map(OpmlResponse)
  }

  def createFrom(opml: String): R = {
    opmlService.create(opml).map { result =>
      result.fold(
        BadRequestResponse,
        ImportResponse
      )
    }
  }

}
