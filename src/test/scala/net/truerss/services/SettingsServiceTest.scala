package net.truerss.services

import net.truerss.dao.FullDbHelper
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.{CheckBoxValue, PredefinedSettings, SelectableValue, UserSettings}
import truerss.dto.{AvailableSetup, CurrentValue, NewSetup, SetupKeys}
import truerss.services.SettingsService

class SettingsServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  import SettingsService._

  sequential

  override def dbName = "settings_service_test"

  private val settingsDao = dbLayer.settingsDao
  private val userSettingsDao = dbLayer.userSettingsDao

  private val default1 = PredefinedSettings(SetupKeys.fFeedParallelism, "desc#1",
    SelectableValue(Iterable(1, 2, 3)))
  private val default2 = PredefinedSettings(SetupKeys.fReadContent, "desc#2",
    CheckBoxValue(false))

  private val userSettings1 = UserSettings(default1.key, default1.description,
    valueInt = Some(10), // somehow
    valueBoolean = None, valueString = None)
  private val v = 123
  // insert new
  a(settingsDao.insert(default1 :: default2 :: Nil))
  a(userSettingsDao.insert(userSettings1 :: Nil))

  private val service = new SettingsService(dbLayer)

  "settings service" should {
    "build available setup" in {
      val current = a(service.getCurrentSetup)
      current ==== Vector(
        AvailableSetup(default1.key, default1.description, default1.toAvailableOptions,
          CurrentValue(userSettings1.valueInt.get)),
        AvailableSetup(default2.key, default2.description, default2.toAvailableOptions,
          CurrentValue(SetupKeys.readContent.value)
        )
      )
    }

    "update current setup" in {

      val newSetup = NewSetup(default1.key, CurrentValue(v))
      val result = a(service.updateSetup(newSetup))
      result ==== 1

      val dbv = a(userSettingsDao.getByKey(default1.key)).get
      dbv.valueInt must beSome(v)

      //
      a(service.updateSetup(NewSetup(default1.key, CurrentValue(Some(v))))) must throwA[IllegalStateException]
    }

    "return key" in {
      val r1 = a(service.where(SetupKeys.parallelism.key).map(_.value))
      val r2 = a(service.where(SetupKeys.readContent.key).map(_.value))
      val r3 = a(service.where(SetupKeys.unknownKey[Any]).map(_.value))
      r1 ==== v
      r2 ==== SetupKeys.readContent.value // default
      r3 ==== null
    }
  }


}
