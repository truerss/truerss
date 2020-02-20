package truerss.services

import truerss.db.DbLayer
import truerss.db.driver.Settings

import scala.concurrent.{ExecutionContext, Future}

class GlobalSettingsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  def getSettings: Future[Iterable[Settings]] = {
    dbLayer.settingsDao.getSettings.map(addDefault)
  }

  def updateSettings(globalSettings: Settings) = {
    ???
  }

  private def addDefault(db: Iterable[Settings]): Iterable[Settings] = {
    val tmpMap = db.map { x => x.key -> x }.toMap
    Settings.predefined.map {
      case c @ Settings(k, _) =>
        tmpMap.getOrElse(k, c)
    }
  }
}
