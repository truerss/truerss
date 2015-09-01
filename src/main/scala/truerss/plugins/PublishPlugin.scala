package truerss.plugins

object PublishActions {
  sealed trait Action
  case object Favorite extends Action
}

/**
 * Created by mike on 1.9.15.
 */
trait PublishPlugin {
  self: ConfigProvider with PluginInfo =>

  import PublishActions._

  def publish(action: Action, entry: Entry): Unit

}
