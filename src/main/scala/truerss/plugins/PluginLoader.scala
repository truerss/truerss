package truerss.plugins

import com.github.truerss.base.{BaseContentPlugin, BaseFeedPlugin, _}
import com.typesafe.config.Config

import java.io.File
import java.net.URLClassLoader
import java.util.jar._
import scala.collection.mutable.ArrayBuffer
import scala.language.existentials

object PluginLoader {

  import JarImplicits._

  private val base = "com.github.truerss.base"
  private val contentPluginName = s"$base.BaseContentPlugin"
  private val feedPluginName = s"$base.BaseFeedPlugin"
  private val publishPluginName = s"$base.BasePublishPlugin"
  private val sitePluginName = s"$base.BaseSitePlugin"

  def load(dirName: String,
           pluginConfig: Config): ApplicationPlugins = {
    val feedPlugins = ArrayBuffer[PluginWithSourcePath[BaseFeedPlugin]]()
    val contentPlugins = ArrayBuffer[PluginWithSourcePath[BaseContentPlugin]]()
    val publishPlugins = ArrayBuffer[PluginWithSourcePath[BasePublishPlugin]]()
    val sitePlugins = ArrayBuffer[PluginWithSourcePath[BaseSitePlugin]]()
    val cssFiles = ArrayBuffer[String]()
    val jsFiles = ArrayBuffer[String]()

    val folder = new File(dirName)

    val jars = folder.listFiles().filter(f =>
      """.*\.jar$""".r.findFirstIn(f.getName).isDefined).toVector

    val packages = jars.map(_.toURI.toURL).toArray

    val classLoader = URLClassLoader.newInstance(packages, getClass.getClassLoader)

    def init[T](c: Class[_]) = {
      val constructor = c.getConstructor(classOf[Config])
      constructor.newInstance(pluginConfig).asInstanceOf[T]
    }

    val initMap = Map(
      `contentPluginName` -> ((clz: Class[_]) => init[BaseContentPlugin](clz)),
      `feedPluginName` -> ((clz: Class[_]) => init[BaseFeedPlugin](clz)),
      `publishPluginName` -> ((clz: Class[_]) => init[BasePublishPlugin](clz)),
      `sitePluginName` -> ((clz: Class[_]) => init[BaseSitePlugin](clz))
    )
    try {
    jars.foreach { file =>
      val sourcePath = file.getAbsolutePath
      val jar = new JarFile(sourcePath)

      val js = jar.reads(".js")

      val css = jar.reads(".css")
      for {entry <- jar.classes.toVector} yield {
        val clz = classLoader.loadClass(entry.klass)
        val superClass = clz.getSuperclass
        Option(superClass).map(_.getCanonicalName).flatMap { x =>
          initMap.get(x).map { f => f.apply(clz) }
        }.foreach { instance =>
          instance match {
            case plugin: BaseContentPlugin =>
              contentPlugins += PluginWithSourcePath(plugin, sourcePath)
            case plugin: BaseFeedPlugin =>
              feedPlugins += PluginWithSourcePath(plugin, sourcePath)
            case plugin: BasePublishPlugin =>
              publishPlugins += PluginWithSourcePath(plugin, sourcePath)
            case plugin: BaseSitePlugin =>
              sitePlugins += PluginWithSourcePath(plugin, sourcePath)
          }
          readAssets(instance, js).foreach(jsFiles += _)
          readAssets(instance, css).foreach(cssFiles += _)
        }
      }
      }
    } catch {
      case _: java.lang.reflect.InvocationTargetException =>
        Console.err.println("Error on plugin initialization")
        sys.exit(1)
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

  private def readAssets(instance: PluginInfo, files: Iterator[String]): Vector[String] = {
    files.map { file =>
      val stream = instance.getClass.getResourceAsStream(s"/$file")
      scala.io.Source.fromInputStream(stream).mkString
    }.toVector
  }
}

object JarImplicits {
  import scala.jdk.CollectionConverters._

  implicit class JarEntryExt(val entry: JarEntry) extends AnyVal {
    def klass: String = {
      entry.getName.split("""/""")
        .mkString(".").replaceAll(""".class""", "")
    }
  }

  implicit class JarFileExt(val jar: JarFile) extends AnyVal {
    def reads(ext: String): Iterator[String] = {
      jar.entries().asScala.filter(x => x.getName.endsWith(ext))
        .map(_.getName)
    }

    def classes: Iterator[JarEntry] = {
      jar.entries().asScala
        .filter(_.getName.contains(".class"))
        .filterNot(_.getName.contains("$"))
    }
  }
}