package net.truerss.tests

import net.truerss.{Resources, ZIOMaterializer}
import org.specs2.mutable.Specification
import truerss.clients.{BadRequestError, SettingsApiHttpClient}
import truerss.db.Predefined
import truerss.dto.{AvailableSelect, CurrentValue, NewSetup}

trait SettingsApiTests extends Specification with Resources {

  import ZIOMaterializer._

  private val settingsClient = new SettingsApiHttpClient(url)

  "settings api" should {
    "scenario" in {
      val current = settingsClient.find.m

      // try to update with invalid value
      val parallelism = current.find(_.key == Predefined.parallelism.key).head

      val parallelismValue = parallelism.value.value.asInstanceOf[Int]

      val value = 999
      val newValue = NewSetup[Int](
        key = parallelism.key,
        value = CurrentValue(value)
      )

      settingsClient.update(Iterable(newValue)).err[BadRequestError] === BadRequestError(Iterable(s"Incorrect value: $value"))
      val newAvailableValue = parallelism.options
        .asInstanceOf[AvailableSelect].predefined.filter(x => x != parallelismValue).head

      val newValue1 = NewSetup[Int](
        key = parallelism.key,
        value = CurrentValue(newAvailableValue)
      )

      settingsClient.update(Iterable(newValue1)).e must beRight

      settingsClient.find.m.find(_.key == Predefined.parallelism.key)
        .head.value.value.asInstanceOf[Int] ==== newAvailableValue
    }
  }


}
