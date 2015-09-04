package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController

import truerss.system.plugins

trait PluginController  extends BaseController with ProxyRefProvider
with ResponseHelper with ActorRefExt {

  import plugins.GetPluginList

  def all = end(GetPluginList)
}
