package truerss.util

import java.io.File
import java.net.{URL, URLClassLoader}
import java.util.jar._

import com.github.truerss.base._
import com.typesafe.config.Config

import scala.collection.mutable.ArrayBuffer
import scala.language.existentials

case class ApplicationPlugins(
  feedPlugins: ArrayBuffer[BaseFeedPlugin] = ArrayBuffer.empty,
  contentPlugins: ArrayBuffer[BaseContentPlugin] = ArrayBuffer.empty,
  publishPlugin: ArrayBuffer[BasePublishPlugin] = ArrayBuffer.empty,
  sitePlugin: ArrayBuffer[BaseSitePlugin] = ArrayBuffer.empty,
  css: ArrayBuffer[String] = ArrayBuffer.empty, // content of js files
  js: ArrayBuffer[String] = ArrayBuffer.empty
) extends Jsonize {
  def matchUrl(url: URL): Boolean = {
    feedPlugins.exists(_.matchUrl(url)) ||
    contentPlugins.exists(_.matchUrl(url)) ||
    sitePlugin.exists(_.matchUrl(url))
  }

  def getFeedReader(url: URL) = {
    (feedPlugins.filter(_.matchUrl(url)) ++
      sitePlugin.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: URL) = {
    (contentPlugins.filter(_.matchUrl(url)) ++
      sitePlugin.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }


}

object PluginLoader {

  import scala.collection.JavaConversions._

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
    val appPlugins = ApplicationPlugins()
    val folder = new File(dirName)

    val jars = folder.listFiles().filter(f =>
      """.*\.jar$""".r.findFirstIn(f.getName).isDefined).toVector

    val packages = jars.map(_.toURI.toURL).toArray

    val classLoader = URLClassLoader.newInstance(packages, getClass.getClassLoader)

    jars.foreach { file =>
      val jar = new JarFile(file.getAbsolutePath)

      val js = jar.entries().filter(x => x.getName.endsWith(".js")).map(_.getName)

      val css = jar.entries().filter(x => x.getName.endsWith(".css")).map(_.getName)

      jar.entries().filter(_.getName.contains(".class"))
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

                appPlugins.contentPlugins += instance
                read(instance, js).map(appPlugins.js += _)
                read(instance, css).map(appPlugins.css += _)

              case `feedPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseFeedPlugin]
                appPlugins.feedPlugins += instance
                read(instance, js).map(appPlugins.js += _)
                read(instance, css).map(appPlugins.css += _)

              case `publishPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BasePublishPlugin]
                appPlugins.publishPlugin += instance
                read(instance, js).map(appPlugins.js += _)
                read(instance, css).map(appPlugins.css += _)

              case `sitePluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseSitePlugin]
                appPlugins.sitePlugin += instance
                read(instance, js).map(appPlugins.js += _)
                read(instance, css).map(appPlugins.css += _)

              case _ =>
            }

          }
        } catch {
          case x: java.lang.reflect.InvocationTargetException =>
            Console.err.println("Error on plugin initialization")
            sys.exit(1)
        }
      }
    }

    appPlugins
  }
}
