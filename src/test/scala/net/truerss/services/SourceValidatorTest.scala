package net.truerss.services

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.db.{DbLayer, SourceDao}
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.dto.SourceDto

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class SourceValidatorTest(implicit ee: ExecutionEnv) extends Specification with Mockito {

  import Gen._

  implicit val dbLayer = mock[DbLayer]
  private val sv = new SourceValidator
  private val duration = 3 seconds

  "SourceValidator" should {
    "error when interval < 0" in {
      val source = genNewSource.copy(interval = 0)
      val result = sv.validate(source)
      result.isLeft must beTrue
      result must beLeft(List("Interval must be great than 0"))
    }

    "error when not valid url" in {
      val source = genNewSource.copy(url = "abc")
      val result = sv.validate(source)
      result must beLeft(List("Not valid url"))
    }

    "error when not valid url and interval" in {
      val source = genNewSource.copy(url = "abc", interval = 0)
      val result = sv.validate(source)
      result must beLeft(List("Interval must be great than 0", "Not valid url"))
    }

    "when valid" in {
      val source = genNewSource
      val result = sv.validate(source)
      result must beRight(source)
    }

    "all is not valid" in {
      val source = genNewSource.copy(url = "abc:)", interval = -1)
      val dbLayerM = mock[DbLayer]
      val sourceDaoM = mock[SourceDao]
      sourceDaoM.findByName(anyString, any[Option[Long]]) returns Future.successful(1)
      sourceDaoM.findByUrl(anyString, any[Option[Long]]) returns Future.successful(1)
      dbLayerM.sourceDao returns sourceDaoM
      val sourceUrlVMock = mock[SourceUrlValidator]
      sourceUrlVMock.validateUrl(any[SourceDto]) returns Left("boom!")
      val validator = new SourceValidator()(dbLayerM, ee.executionContext) {
        override protected val sourceUrlValidator = sourceUrlVMock
      }
      val result = Await.result(validator.validateSource(source), duration)
      result must beLeft
      val xs = result.swap.toOption.get
      xs must have size 5
    }
  }

}
