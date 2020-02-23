package truerss.services

import truerss.db.DbLayer
import truerss.db.driver.{PredefinedSettings, PredefinedSettings}

import scala.concurrent.{ExecutionContext, Future}

class GlobalSettingsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  def getSettings: Future[Iterable[PredefinedSettings]] = {
    dbLayer.settingsDao.getSettings.map(addDefault)
  }

  def updateSettings(globalSettings: PredefinedSettings) = {
    ???
  }

  private def addDefault(db: Iterable[PredefinedSettings]): Iterable[PredefinedSettings] = {
    val tmpMap = db.map { x => x.key -> x }.toMap
    PredefinedSettings.predefined.map {
      case c @ PredefinedSettings(k, _) =>
        tmpMap.getOrElse(k, c)
    }
  }
}
