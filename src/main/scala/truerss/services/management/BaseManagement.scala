package truerss.services.management

import org.slf4j.LoggerFactory
import truerss.api.Response

import scala.concurrent.Future

trait BaseManagement {

  type R = Future[Response]

  protected val logger = LoggerFactory.getLogger(getClass)

}
