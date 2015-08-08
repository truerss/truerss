package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.routing.HttpService
import truerss.models.ApiJsonProtocol
import truerss.system.db

/**
 * Created by mike on 1.8.15.
 */
trait FeedController extends BaseController with ProxyRefProvider
  with ResponseHelper with ActorRefExt {

  import HttpService._
  //import spray.json._
  //import ApiJsonProtocol._
  import db._

  def favorites = end(Favorites)

}
