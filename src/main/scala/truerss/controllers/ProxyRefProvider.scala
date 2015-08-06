package truerss.controllers

import akka.actor.ActorRefFactory

/**
 * Created by mike on 2.8.15.
 */
trait ProxyRefProvider {
  val proxyRef: akka.actor.ActorRef
  val context: ActorRefFactory
}
