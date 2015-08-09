package truerss.system

import akka.actor.Actor
import akka.event.LoggingReceive


/**
 * Created by mike on 9.8.15.
 *
 */
class NetworkActor extends Actor {

  import network._

  def receive = LoggingReceive {
    case Grep(url) => 

    case ExtractContent(url) =>
  }

}
