package net.truerss.services.actors

import scala.concurrent.Future

trait ActorTestHelper {

  def f[T](x: T) = Future.successful(x)

}
