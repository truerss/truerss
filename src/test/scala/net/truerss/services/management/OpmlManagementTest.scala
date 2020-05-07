package net.truerss.services.management

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.ImportResponse
import truerss.dto.{NewSourceDto, NewSourceFromFileWithErrors}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.management.OpmlManagement
import truerss.services.{OpmlService, SourcesService}
import truerss.util.Outline

import scala.concurrent.Future

class OpmlManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  private val validOpml = "test"
  private val os = mock[OpmlService]
  private val ss = mock[SourcesService]
  private val es = mock[EventStream]
  private val o1 = Outline(
    title = "test title",
    link = Gen.genUrl
  )
  private val o2 = Outline(
    title = "test title",
    link = Gen.genUrl
  )
  private val dto = Gen.genView
  os.parse(validOpml) returns Right(Iterable(o1, o2))

  ss.addSources(any[Iterable[NewSourceDto]])
    .returns(f(Iterable(dto)))

  private val om = new OpmlManagement(os, ss, es)

  "opml" should {
    "return only valid sources" in {
      om.createFrom(validOpml) must be_==(
        ImportResponse(Vector(dto))).await

      there was one(ss).addSources(any[Iterable[NewSourceDto]])
      there was one(es).publish(SourcesKeeperActor.NewSource(dto))
    }
  }

  private def f[T](x: T) = Future.successful(x)

}
