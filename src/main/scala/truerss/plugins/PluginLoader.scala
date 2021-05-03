package truerss.plugins

import com.github.truerss.base.{BaseFeedPlugin, _}
import com.typesafe.config.Config

import java.io.File
import java.net.URLClassLoader
import java.util.jar._
import scala.collection.mutable.ArrayBuffer
import scala.language.existentials

object PluginLoader {

  import scala.jdk.CollectionConverters._
  import JarImplicits._

  val base = "com.github.truerss.base"
  val contentPluginName = s"$base.BaseContentPlugin"
  val feedPluginName = s"$base.BaseFeedPlugin"
  val publishPluginName = s"$base.BasePublishPlugin"
  val sitePluginName = s"$base.BaseSitePlugin"

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

    jars.foreach { file =>
      val sourcePath = file.getAbsolutePath
      val jar = new JarFile(sourcePath)

      val js = jar.reads(".js")

      val css = jar.reads(".css")

      jar.classes.foreach { entry =>
        val clz = classLoader.loadClass(entry.klass)
        try {
          val superClass = clz.getSuperclass

          if (superClass != null) {
            superClass.getCanonicalName match {
              case `contentPluginName` =>
                val instance = init[BaseContentPlugin](clz)
                contentPlugins += PluginWithSourcePath(instance, sourcePath)
                readAssets(instance, js).foreach(jsFiles += _)
                readAssets(instance, css).foreach(cssFiles += _)

              case `feedPluginName` =>
                val instance = init[BaseFeedPlugin](clz)
                feedPlugins += PluginWithSourcePath(instance, sourcePath)
                readAssets(instance, js).foreach(jsFiles += _)
                readAssets(instance, css).foreach(cssFiles += _)

              case `publishPluginName` =>
                val instance = init[BasePublishPlugin](clz)
                publishPlugins += PluginWithSourcePath(instance, sourcePath)
                readAssets(instance, js).foreach(jsFiles += _)
                readAssets(instance, css).foreach(cssFiles += _)

              case `sitePluginName` =>
                val instance = init[BaseSitePlugin](clz)
                sitePlugins += PluginWithSourcePath(instance, sourcePath)
                readAssets(instance, js).foreach(jsFiles += _)
                readAssets(instance, css).foreach(cssFiles += _)

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