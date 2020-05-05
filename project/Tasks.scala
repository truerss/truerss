import sbt.Keys._
import sbt._
import java.nio.file.{Files, Paths}
import java.nio.charset.StandardCharsets

object Tasks {
  import sys.process._

  val cdnjs = "https://cdnjs.cloudflare.com/ajax/libs"

  val momentJsVersion = "2.17.1"
  //https://cdnjs.cloudflare.com/ajax/libs/uikit/3.0.0-beta.2/css/uikit.css
  val uiKitVersion = "3.4.2" // 2.22.0

  val jsLibs = Seq(
    "https://code.jquery.com/jquery-2.1.4.min.js",
    "https://github.com/mde/ejs/releases/download/v2.3.4/ejs.min.js",
    s"https://cdnjs.cloudflare.com/ajax/libs/moment.js/$momentJsVersion/moment.min.js",
    "https://raw.githubusercontent.com/fntz/sirius/master/sirius.min.js",
    "https://raw.githubusercontent.com/fntz/sirius/master/jquery_adapter.min.js",
    s"$cdnjs/uikit/$uiKitVersion/js/components/notification.min.js",
    s"$cdnjs/uikit/$uiKitVersion/js/uikit.min.js",
    s"$cdnjs/uikit/$uiKitVersion/js/components/upload.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/tippy.js/3.0.6/tippy.standalone.min.js"
  )

  val cssLibs = Seq(
//    s"$cdnjs/uikit/$uiKitVersion/css/components/form-file.min.css",
//    s"$cdnjs/uikit/$uiKitVersion/css/components/notify.min.css",
//    s"$cdnjs/uikit/$uiKitVersion/css/components/placeholder.min.css",
//    s"$cdnjs/uikit/$uiKitVersion/css/components/upload.min.css",
//    s"$cdnjs/uikit/$uiKitVersion/css/components/search.min.css",
    s"$cdnjs/uikit/$uiKitVersion/css/uikit.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/tippy.js/3.0.6/tippy.css"
  )

  val fonts = Seq(
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/fonts/FontAwesome.otf",
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/fonts/fontawesome-webfont.eot",
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/fonts/fontawesome-webfont.ttf",
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/fonts/fontawesome-webfont.ttf",
    "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/fonts/fontawesome-webfont.woff2"
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
      "main_controller" :: "models" :: "sources_controller" :: "settings_controller" ::
      "plugins_controller" :: "upload_controller" ::
      "templates" :: "app" :: Nil

    val rfiles = files.map { f => s"$path/$f.coffee" }.mkString(" ")

    val fileName = s"$c_to/truerss.js"

    val content = s"cat $rfiles" #| "coffee -c -b --stdio"

    val result = content.lineStream.mkString("\n")

    Files.write(Paths.get(fileName), result.getBytes(StandardCharsets.UTF_8))


    println("Done")
  }
}