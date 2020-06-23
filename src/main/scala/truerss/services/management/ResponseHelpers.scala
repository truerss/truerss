package truerss.services.management

import truerss.api.{NotFoundResponse, Ok}

object ResponseHelpers {
  val ok = Ok("ok")
  val sourceNotFound = NotFoundResponse("Source not found")
  val feedNotFound = NotFoundResponse("Feed not found")
}