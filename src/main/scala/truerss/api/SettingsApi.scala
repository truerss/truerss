package truerss.api

import truerss.dto.{AvailableSetup, NewSetup}
import truerss.services.SettingsService
import com.github.fntz.omhs.{BodyReader, BodyWriter, ParamDSL}
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.playjson.JsonSupport

class SettingsApi(private val settingsService: SettingsService) extends HttpApi {

  import JsonFormats._
  import ParamDSL._
  import RoutingImplicits._
  import ZIOSupport._

  private val ss = settingsService

  private type Z = List[NewSetup[_]]

  implicit val currentSetupWriter: BodyWriter[Iterable[AvailableSetup[_]]] =
    JsonSupport.writer[Iterable[AvailableSetup[_]]]

  implicit val newSetupReader: BodyReader[Z] =
    JsonSupport.reader[Z]

  case class Setups(included: Iterable[NewSetup[_]])

  implicit lazy val setupsReader: BodyReader[Setups] = new BodyReader[Setups] {
    override def read(str: String): Setups = {
      Setups(newSetupReader.read(str))
    }
  }

  private val settings = get("api" / "v1" / "settings" / "current") ~> {() =>
    ss.getCurrentSetup
  }

  private val updateSettings = put("api" / "v1" / "settings" / body[Setups]) ~> { (ns: Setups) =>
    ss.updateSetups(ns.included)
  }

  val route = ???


}
