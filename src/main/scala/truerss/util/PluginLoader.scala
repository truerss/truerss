package truerss.util

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar._

import com.github.truerss.base.{BaseFeedPlugin, _}
import com.typesafe.config.Config

import scala.collection.mutable.ArrayBuffer
import scala.language.existentials
import scala.util.Try

case class ApplicationPlugins(
                               feedPlugins: Vector[BaseFeedPlugin] = Vector.empty,
                               contentPlugins: Vector[BaseContentPlugin] = Vector.empty,
                               publishPlugins: Vector[BasePublishPlugin] = Vector.empty,
                               sitePlugins: Vector[BaseSitePlugin] = Vector.empty,
                               css: Vector[String] = Vector.empty, // content of js files
                               js: Vector[String] = Vector.empty
) {
  def matchUrl(url: URL): Boolean = {
    feedPlugins.exists(_.matchUrl(url)) ||
    contentPlugins.exists(_.matchUrl(url)) ||
    sitePlugins.exists(_.matchUrl(url))
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(new java.net.URL(url))).toOption.getOrElse(false)
  }

  def addPlugin(plugin: BasePlugin): ApplicationPlugins = {
    plugin match {
      case x: BaseFeedPlugin =>
        copy(feedPlugins = feedPlugins :+ x)
      case x: BaseContentPlugin =>
        copy(contentPlugins = contentPlugins :+ x)
      case x: BasePublishPlugin =>
        copy(publishPlugins = publishPlugins :+ x)
      case x: BaseSitePlugin =>
        copy(sitePlugins = sitePlugins :+ x)
    }
  }

}

object PluginLoader {

  import scala.collection.JavaConverters._

  val contentPluginName = "com.github.truerss.base.BaseContentPlugin"
  val feedPluginName = "com.github.truerss.base.BaseFeedPlugin"
  val publishPluginName = "com.github.truerss.base.BasePublishPlugin"
  val sitePluginName = "com.github.truerss.base.BaseSitePlugin"

  def read(instance: PluginInfo, files: Iterator[String]): Vector[String] = {
    files.map { file =>
      val stream = instance.getClass.getResourceAsStream(s"/$file")
      scala.io.Source.fromInputStream(stream).mkString
    }.toVector
  }

  def load(dirName: String,
           pluginConfig: Config): ApplicationPlugins = {
    val feedPlugins = ArrayBuffer[BaseFeedPlugin]()
    val contentPlugins = ArrayBuffer[BaseContentPlugin]()
    val publishPlugins = ArrayBuffer[BasePublishPlugin]()
    val sitePlugins = ArrayBuffer[BaseSitePlugin]()
    val cssFiles = ArrayBuffer[String]()
    val jsFiles = ArrayBuffer[String]()

    val folder = new File(dirName)

    val jars = folder.listFiles().filter(f =>
      """.*\.jar$""".r.findFirstIn(f.getName).isDefined).toVector

    val packages = jars.map(_.toURI.toURL).toArray

    val classLoader = URLClassLoader.newInstance(packages, getClass.getClassLoader)

    jars.foreach { file =>
      val jar = new JarFile(file.getAbsolutePath)

      val js = jar.entries().asScala.filter(x => x.getName.endsWith(".js")).map(_.getName)

      val css = jar.entries().asScala.filter(x => x.getName.endsWith(".css")).map(_.getName)

      jar.entries().asScala.filter(_.getName.contains(".class"))
      .filterNot(_.getName.contains("$")).foreach { entry =>
        val cc = entry.getName.split("""/""")
          .mkString(".").replaceAll(""".class""", "")
        val clz = classLoader.loadClass(cc)
        try {
          val superClass = clz.getSuperclass
          if (superClass != null) {
            superClass.getCanonicalName match {
              case `contentPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseContentPlugin]

                contentPlugins += instance
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `feedPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseFeedPlugin]
                feedPlugins += instance
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `publishPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BasePublishPlugin]
                publishPlugins += instance
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `sitePluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseSitePlugin]
                sitePlugins += instance
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case _ =>
            }

          }
        } catch {
          case _: java.lang.reflect.InvocationTargetException =>
            Console.err.println("Error on plugin initialization")
            sys.exit(1)
        }
      }
    }

    ApplicationPlugins(
      feedPlugins = feedPlugins.toVector,
      contentPlugins = contentPlugins.toVector,
      publishPlugins = publishPlugins.toVector,
      sitePlugins = sitePlugins.toVector,
      css = cssFiles.toVector,
      js = jsFiles.toVector
    )
  }
}
