package net.truerss.services

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import truerss.db.{DbLayer, UserSettingsDao}
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.SettingsService
import truerss.util.SettingsImplicits
import net.truerss.ZIOMaterializer
import zio.Task

class SettingsServiceTests extends Specification with Mockito {

  import SettingsImplicits._
  import SettingsService._
  import Reducer._
  import ZIOMaterializer._

  private class Test extends Scope {
    val dbLayer = mock[DbLayer]
    val dao = mock[UserSettingsDao]
    dbLayer.userSettingsDao returns dao

    var service: SettingsService = _

    def init(setups: Iterable[AvailableSetup[_]]) = {
      service = new SettingsService(dbLayer) {
        override def getCurrentSetup: Task[Iterable[AvailableSetup[_]]] = {
          Task.succeed(setups)
        }
      }
    }

  }

  "SettingsService" should {
    "updateSetups" should {
      "when invalid" in new Test() {
        val availableSetups = Iterable(
          AvailableSetup(
            key = "test#1",
            description = "test#1",
            options = AvailableSelect(Iterable(1, 2, 3)),
            value = CurrentValue(1)
          )
        )
        val newSetups = Iterable(
          NewSetup("test#1", CurrentValue(10))
        )

        init(availableSetups)

        service.updateSetups(newSetups).e must beLeft

        there was no(dbLayer)
      }

      "success flow" in new Test() {
        val availableSetups = Iterable(
          AvailableSetup(
            key = "test#1",
            description = "test#1",
            options = AvailableSelect(Iterable(1, 2, 3)),
            value = CurrentValue(1)
          )
        )
        val newSetup = NewSetup("test#1", CurrentValue(1))
        val newSetups = Iterable(newSetup)

        dao.bulkUpdate(Iterable(newSetup.toUserSetup)) returns Task.succeed(())

        init(availableSetups)

        service.updateSetups(newSetups).e must beRight

        there was one(dao).bulkUpdate(Iterable(newSetup.toUserSetup))
      }
    }

    "#reduce" in {
      val currentSetups = Iterable(
        AvailableSetup(
          key = "test#1",
          description = "test#1",
          options = AvailableSelect(Iterable(1, 2, 3)),
          value = CurrentValue(1)
        ),
        AvailableSetup(
          key = "test#2",
          description = "test#2",
          options = AvailableSelect(Iterable(10, 20, 30)),
          value = CurrentValue(10)
        )
      )
      val valid = NewSetup("test#2", CurrentValue(30))
      val invalid = NewSetup("test#1", CurrentValue(10))
      val newSetups = Iterable(
        NewSetup("test#1", CurrentValue(10)),
        NewSetup("test#2", CurrentValue(30)),
        NewSetup("test#3", CurrentValue(100))
      )

      val result = reprocess(currentSetups, newSetups)

      result.setups ==== Vector(valid.toUserSetup)
      result.errors ==== Vector(buildError(invalid.value))
    }
  }

}
