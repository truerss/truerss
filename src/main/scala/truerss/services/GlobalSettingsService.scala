package truerss.services

import truerss.db.DbLayer
import truerss.db.driver.GlobalSettings

import scala.concurrent.{ExecutionContext, Future}

class GlobalSettingsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  def getSettings: Future[Iterable[GlobalSettings]] = {
    dbLayer.globalSettingsDao.getGlobalSettings
  }

}
