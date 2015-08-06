package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController

import spray.routing.HttpService
/**
 * Created by mike on 1.8.15.
 */
trait MainController extends BaseController {

  import HttpService._

  def root = complete("123")

}
