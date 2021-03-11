package truerss.api

import com.github.fntz.omhs.playjson.JsonSupport
import com.github.fntz.omhs.{BodyReader, BodyWriter, RoutingDSL}
import truerss.dto.{AvailableSetup, NewSetup}
import truerss.services.SettingsService


class SettingsApi(private val settingsService: SettingsService) extends HttpApi {

  import JsonFormats._
  import RoutingDSL._
  import ZIOSupport._

  private val ss = settingsService

  private type Z = List[NewSetup[_]]

  implicit val currentSetupWriter: BodyWriter[Iterable[AvailableSetup[_]]] =
    JsonSupport.writer[Iterable[AvailableSetup[_]]]

  implicit val newSetupReader: BodyReader[Z] =
    JsonSupport.reader[Z]

  case class Setups(included: Iterable[NewSetup[_]])

  implicit lazy val setupsReader: BodyReader[Setups] = (str: String) => {
    Setups(newSetupReader.read(str))
  }

  private val settings = get("api" / "v1" / "settings" / "current") ~> {() =>
    w(ss.getCurrentSetup)
  }

  private val updateSettings = put("api" / "v1" / "settings" <<< body[Setups]) ~> { (ns: Setups) =>
    w(ss.updateSetups(ns.included))
  }

  val route = settings :: updateSettings


}
