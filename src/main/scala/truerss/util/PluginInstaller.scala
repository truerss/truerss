package truerss.util

import org.slf4j.LoggerFactory
import truerss.services.PluginNotFoundError

import sys.process._
import zio.Task

import java.io.File
import java.net.URL

class PluginInstaller(private val pluginHomeDir: String) {
  import PluginInstaller._

  private val logger = LoggerFactory.getLogger(getClass)

  // todo Request
  def install(urlToJar: String): Task[Unit] = {
    val fileName = toFilePath(pluginHomeDir, urlToJar)
    for {
      _ <- Task.effectTotal(logger.debug(s"Install $urlToJar to $fileName"))
      _ <- Task {
        new URL(urlToJar) #> new File(fileName) !!
      }
    } yield ()
  }

  def remove(urlToJar: String): Task[Unit] = {
    val fileName = toFilePath(pluginHomeDir, urlToJar)
    val file = new File(fileName)
    if (file.exists()) {
      Task(file.delete())
    } else {
      Task.fail(PluginNotFoundError)
    }
  }

}

object PluginInstaller {
  def toFilePath(pluginHomeDir: String, urlToJar: String): String = {
    s"$pluginHomeDir/${urlToJar.split("/").last}"
  }
}
