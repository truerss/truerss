package truerss.util

import org.slf4j.LoggerFactory
import truerss.services.PluginNotFoundError

import sys.process._
import zio.{Task, ZIO}

import java.io.File
import java.net.URI

class PluginInstaller(private val pluginHomeDir: String) {
  import PluginInstaller._

  private val logger = LoggerFactory.getLogger(getClass)

  // todo Request
  def install(urlToJar: String): Task[Unit] = {
    val fileName = toFilePath(pluginHomeDir, urlToJar)
    for {
      _ <- ZIO.succeed(logger.debug(s"Install $urlToJar to $fileName"))
      _ <- ZIO.attempt {
        URI.create(urlToJar).toString #> new File(fileName) !!
      }
    } yield ()
  }

  def remove(urlToJar: String): Task[Unit] = {
    val fileName = toFilePath(pluginHomeDir, urlToJar)
    val file = new File(fileName)
    if (file.exists()) {
      ZIO.attempt(file.delete())
    } else {
      ZIO.fail(PluginNotFoundError)
    }
  }

}

object PluginInstaller {
  def toFilePath(pluginHomeDir: String, urlToJar: String): String = {
    s"$pluginHomeDir/${urlToJar.split("/").last}"
  }
}
