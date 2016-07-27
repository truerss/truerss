package truerss.system.actors

import truerss.controllers.BadRequestResponse
import truerss.models.Source

import scala.concurrent.Future

trait AddOrUpdateFSMHelpers {
  def asBadResponse(x: String) = Future.successful(BadRequestResponse(x))
  def urlError(source: Source) = s"Url '${source.url}' already present in db"
  def nameError(source: Source) = s"Name '${source.name}' not unique"
}
