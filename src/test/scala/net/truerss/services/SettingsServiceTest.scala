package net.truerss.services

import net.truerss.dao.FullDbHelper
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.{Predefined, UserSettings}
import truerss.dto._
import truerss.services.SettingsService

class SettingsServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  import SettingsService._

  sequential

  override def dbName = "settings_service_test"

  private val settingsDao = dbLayer.settingsDao
  private val userSettingsDao = dbLayer.userSettingsDao

  private val default1 = Predefined.parallelism
  private val default2 = Predefined.read
  private val default3 = Predefined.feedsPerPage

  private val v = 123
  private val userSettings1 = UserSettings(default1.key, default1.description,
    valueInt = Some(v), // somehow
    valueBoolean = None, valueString = None)

  // insert new
  a(settingsDao.insert(default1 :: default2 :: default3 :: Nil))
  a(userSettingsDao.insert(userSettings1 :: Nil))

  private val service = new SettingsService(dbLayer)

  "settings service" should {
    "build available setup" in {
      val current = a(service.getCurrentSetup)
      current ==== Vector(
        AvailableSetup(default1.key, default1.description, default1.toAvailableOptions,
          CurrentValue(userSettings1.valueInt.get)),
        AvailableSetup(default2.key, default2.description, default2.toAvailableOptions,
          CurrentValue(default2.value.defaultValue)),
          AvailableSetup(default3.key, default3.description, default3.toAvailableOptions,
            CurrentValue(default3.value.defaultValue)
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
      val r1 = a(service.where[Int](SetupKey(Predefined.parallelism.key,
        Predefined.parallelism.description), 12).map(_.value))
      val r2 = a(service.where[Boolean](SetupKey(Predefined.read.key,
        Predefined.read.description), false).map(_.value))
      val r3 = a(service.where[String](Setup.unknown[String]("test").key, "test").map(_.value))

      r1 ==== v
      r2 ==== true
      r3 ==== "test"
    }
  }


}
