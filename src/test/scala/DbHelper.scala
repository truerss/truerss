import truerss.db.{H2, DBProfile}
import truerss.models.CurrentDriver

import scala.slick.jdbc.JdbcBackend

trait DbHelper {

  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = dbProfile.driver)
  val driver = new CurrentDriver(dbProfile.profile)


}
