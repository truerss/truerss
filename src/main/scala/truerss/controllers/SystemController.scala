package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import truerss.system.global

trait SystemController extends BaseController with ProxyRefProvider
with ResponseHelper with ActorRefExt {

  import global._

  def stop = end(StopSystem)

  def restart = end(RestartSystem)

  def exit = end(StopApp)

}
