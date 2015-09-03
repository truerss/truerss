package truerss.util

import java.io.File
import java.net.URLClassLoader
import java.util.jar._

import com.github.truerss.base.{BaseContentPlugin, BaseFeedPlugin, BasePublishPlugin, BaseSitePlugin}

import scala.collection.mutable.ArrayBuffer
import scala.language.existentials

case class ApplicationPlugins(
  feedPlugins: ArrayBuffer[BaseFeedPlugin] = ArrayBuffer.empty,
  contentPlugins: ArrayBuffer[BaseContentPlugin] = ArrayBuffer.empty,
  publishPlugin: ArrayBuffer[BasePublishPlugin] = ArrayBuffer.empty,
  sitePlugin: ArrayBuffer[BaseSitePlugin] = ArrayBuffer.empty
) {
  def matchUrl(url: String): Boolean = {
    feedPlugins.exists(_.matchUrl(url)) ||
    contentPlugins.exists(_.matchUrl(url)) ||
    sitePlugin.exists(_.matchUrl(url))
  }

  def getFeedReader(url: String) = {
    (feedPlugins.filter(_.matchUrl(url)) ++
      sitePlugin.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: String) = {
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

  def load(dirName: String,
           pluginSetting: Map[String, Map[String, String]]): ApplicationPlugins = {
    val appPlugins = ApplicationPlugins()
    val folder = new File(dirName)

    val jars = folder.listFiles().filter(f =>
      """.*\.jar$""".r.findFirstIn(f.getName).isDefined).toVector

    val packages = jars.map(_.toURI.toURL).toArray

    val classLoader = URLClassLoader.newInstance(packages, getClass.getClassLoader)

    jars.foreach { file =>
      val jar = new JarFile(file.getAbsolutePath)

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
                val param = Map.empty
                val constructor = clz.getConstructor(classOf[Map[String, String]])
                appPlugins.contentPlugins += constructor.newInstance(param)
                  .asInstanceOf[BaseContentPlugin]

              case `feedPluginName` =>
                val param = Map.empty
                val constructor = clz.getConstructor(classOf[Map[String, String]])
                appPlugins.feedPlugins += constructor.newInstance(param)
                  .asInstanceOf[BaseFeedPlugin]

              case `publishPluginName` =>
                val param = Map.empty
                val constructor = clz.getConstructor(classOf[Map[String, String]])
                appPlugins.publishPlugin += constructor.newInstance(param)
                  .asInstanceOf[BasePublishPlugin]

              case `sitePluginName` =>
                val param = Map.empty
                val constructor = clz.getConstructor(classOf[Map[String, String]])
                appPlugins.sitePlugin += constructor.newInstance(param)
                  .asInstanceOf[BaseSitePlugin]

              case _ =>
            }

          }
        } catch {
          case _: java.lang.reflect.InvocationTargetException =>
            Console.err.println("====")
            sys.exit(1)
        }
      }
    }

    appPlugins
  }
}

















