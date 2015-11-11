import java.io.FileWriter

import sbt.Keys._
import sbt._

import scala.io.Source

object Tasks {
  val jsLibs = Seq(
    "https://code.jquery.com/jquery-2.1.4.min.js",
    "https://github.com/mde/ejs/releases/download/v2.3.4/ejs.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/lodash.js/3.10.1/lodash.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.10.6/moment.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/components/notify.min.js",
    "https://raw.githubusercontent.com/fntz/sirius/master/sirius.min.js",
    "https://raw.githubusercontent.com/fntz/sirius/master/jquery_adapter.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/uikit.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/js/components/upload.min.js"
  )

  val cssLibs = Seq(
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/form-file.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/notify.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/placeholder.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/components/upload.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/uikit/2.22.0/css/uikit.almost-flat.min.css"
  )

  val fonts = Seq(
    "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/fonts/FontAwesome.otf",
    "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/fonts/fontawesome-webfont.eot",
    "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/fonts/fontawesome-webfont.ttf",
    "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/fonts/fontawesome-webfont.woff",
    "https://raw.githubusercontent.com/FortAwesome/Font-Awesome/master/fonts/fontawesome-webfont.woff2"
  )

  def download(url: String, dir: String): Unit = {
    val fileName = url.split("""/""").last
    val file = s"$dir$fileName"
    val pf = new File(file)
    if (pf.exists()) {
      pf.delete()
    }
    val fw = new FileWriter(file, true)
    println(s"Download $fileName")
    val content = Source.fromURL(new URL(url)).mkString
    fw.write(content)
    fw.close()
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
    import sys.process._

    "rake"!

    println("Done")
  }
}