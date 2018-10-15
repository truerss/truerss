import slick.jdbc.JdbcBackend
import truerss.db.drivers.{CurrentDriver, DBProfile, H2}

trait DbHelper {

  def dbName: String = "test1"

  val dbProfile = DBProfile.create(H2)
  def db = JdbcBackend.Database
    .forURL(s"jdbc:h2:mem:$dbName;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
      driver = dbProfile.driver)
  implicit val driver = CurrentDriver(dbProfile.profile)

}
