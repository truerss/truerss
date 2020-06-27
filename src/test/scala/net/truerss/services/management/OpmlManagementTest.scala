package net.truerss.services.management

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.ImportResponse
import truerss.services.management.OpmlManagement
import truerss.services.OpmlService

import scala.concurrent.Future

class OpmlManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  private val validOpml = "test"
  private val os = mock[OpmlService]
  private val dto = Gen.genView
  os.create(validOpml) returns Future.successful(Right(Iterable(dto)))

  private val om = new OpmlManagement(os)

  "opml" should {
    "transform" in {
      om.createFrom(validOpml) must be_==(
        ImportResponse(Vector(dto))).await
    }
  }

  private def f[T](x: T) = Future.successful(x)

}
