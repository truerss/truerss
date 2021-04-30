package truerss.util

import org.slf4j.LoggerFactory

import sys.process._
import zio.Task

import java.io.File
import java.net.URL

class PluginInstaller(private val pluginHomeDir: String) {

  private val logger = LoggerFactory.getLogger(getClass)

  // todo Request
  def install(urlToJar: String): Task[Unit] = {
    val fileName = s"$pluginHomeDir/${urlToJar.split("/").last}"
    for {
      _ <- Task.effectTotal(logger.debug(s"Install $urlToJar to $fileName"))
      _ <- Task {
        new URL(urlToJar) #> new File(fileName) !!
      }
    } yield ()
  }

}
