package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.routing.HttpService

/**
 * Created by mike on 1.8.15.
 */
trait FeedController extends BaseController with ProxyRefProvider {

  import HttpService._

  def favorites = complete("dsa")

}
