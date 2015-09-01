package truerss.plugins

/**
 * Created by mike on 1.9.15.
 */
trait ConfigProvider {
  val config: Map[String, String] = Map.empty
}
