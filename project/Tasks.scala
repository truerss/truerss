import java.io.FileWriter

import sbt.Keys._
import sbt._

import scala.io.Source

object Tasks {
  import sys.process._

  val jsLibs = Seq(
    "http://code.jquery.com/jquery-2.1.4.min.js",
    "http://github.com/mde/ejs/releases/download/v2.3.4/ejs.min.js",
    "http://cdnjs.cloudflare.com/ajax/libs/moment.js/2.10.6/moment.min.js",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/components/notify.min.js",
    "http://raw.githubusercontent.com/fntz/sirius/master/sirius.min.js",
    "http://raw.githubusercontent.com/fntz/sirius/master/jquery_adapter.min.js",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/uikit.min.js",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/components/upload.min.js"
  )

  val cssLibs = Seq(
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/form-file.min.css",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/notify.min.css",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/placeholder.min.css",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/upload.min.css",
    "http://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/uikit.almost-flat.min.css"
  )

  val fonts = Seq(
    "http://github.com/FortAwesome/Font-Awesome/raw/master/fonts/FontAwesome.otf",
    "http://github.com/FortAwesome/Font-Awesome/raw/master/fonts/fontawesome-webfont.eot",
    "http://github.com/FortAwesome/Font-Awesome/raw/master/fonts/fontawesome-webfont.ttf",
    "http://github.com/FortAwesome/Font-Awesome/raw/master/fonts/fontawesome-webfont.woff",
    "http://github.com/FortAwesome/Font-Awesome/raw/master/fonts/fontawesome-webfont.woff2"
  )

  def download(url: String, dir: String): Unit = {
    val fileName = url.split("""/""").last
    val d = new File(dir)
    if (!d.exists()) {
      d.mkdir()
    }
    val file = s"$dir$fileName"
    val pf = new File(file)
    if (pf.exists()) {
      pf.delete()
    }
    println(s"Download $fileName")

    new URL(url) #> pf !!
  }

  val install = TaskKey[Unit]("install", "install all dependencies for web ui")
  val installTask = install := {

    val res = "src/main/resources"
    val currentPath = new File(".").getAbsolutePath

    def p(x: String) = s"$currentPath/$res/$x/".replaceAll("""\/\.\/""", "/")

    val jsPath = p("javascript")
    val cssPath = p("css")
    val fontsPath = p("fonts")

    Seq(jsLibs, cssLibs, fonts) zip Seq(jsPath, cssPath, fontsPath) foreach { case p @ (libs, path) =>
      libs.foreach(download(_, path))
    }

    println("Done")
  }

  val buildCoffee = TaskKey[Unit]("jsbuild", "compile coffeescript")
  val buildCoffeeTask = buildCoffee := {
    println("Compile Coffeescript")

    val pwd = baseDirectory.value.getAbsolutePath
    val c_to = s"$pwd/src/main/resources/javascript/"
    val path = s"$pwd/src/main/resources/javascript/app"

    val files = "ext" :: "feeds_controller" :: "ws_controller" :: "contoller_ext" ::
      "main_controller" :: "models" :: "sources_controller" :: "system_controller" ::
      "templates" :: "app" :: Nil

    val rfiles = files.map { f => s"$path/$f.coffee" }.mkString(" ")

    val result = new File(s"$c_to/truerss.js")

    s"cat $rfiles" #| "coffee -c -b --stdio" #> result !

    println("Done")
  }
}