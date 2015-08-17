package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController

import spray.routing.HttpService
/**
 * Created by mike on 1.8.15.
 */
trait MainController extends BaseController {

  import HttpService._
  import spray.httpx.TwirlSupport._

  def root = complete(truerss.html.index.render)

}
