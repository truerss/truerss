package truerss

import com.github.fntz.omhs.OMHSServer
import truerss.db.DbLayer

case class AppInstance(server: OMHSServer.Instance, dbLayer: DbLayer) {
  def start(): Unit = server.start()
  def stop(): Unit = server.stop()
}
