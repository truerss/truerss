package truerss.plugins

import com.github.truerss.base.{BaseFeedPlugin, _}
import com.typesafe.config.Config
import truerss.dto.{ApplicationPlugins, PluginWithSourcePath}

import java.io.File
import java.net.URLClassLoader
import java.util.jar._
import scala.collection.mutable.ArrayBuffer
import scala.language.existentials

object PluginLoader {

  import scala.jdk.CollectionConverters._

  val base = "com.github.truerss.base"
  val contentPluginName = s"$base.BaseContentPlugin"
  val feedPluginName = s"$base.BaseFeedPlugin"
  val publishPluginName = s"$base.BasePublishPlugin"
  val sitePluginName = s"$base.BaseSitePlugin"

  def read(instance: PluginInfo, files: Iterator[String]): Vector[String] = {
    files.map { file =>
      val stream = instance.getClass.getResourceAsStream(s"/$file")
      scala.io.Source.fromInputStream(stream).mkString
    }.toVector
  }

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

    jars.foreach { file =>
      val sourcePath = file.getAbsolutePath
      val jar = new JarFile(sourcePath)

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

                contentPlugins += PluginWithSourcePath(instance, sourcePath)
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `feedPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseFeedPlugin]
                feedPlugins += PluginWithSourcePath(instance, sourcePath)
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `publishPluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BasePublishPlugin]
                publishPlugins += PluginWithSourcePath(instance, sourcePath)
                read(instance, js).foreach(jsFiles += _)
                read(instance, css).foreach(cssFiles += _)

              case `sitePluginName` =>
                val constructor = clz.getConstructor(classOf[Config])
                val instance = constructor.newInstance(pluginConfig)
                  .asInstanceOf[BaseSitePlugin]
                sitePlugins += PluginWithSourcePath(instance, sourcePath)
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
