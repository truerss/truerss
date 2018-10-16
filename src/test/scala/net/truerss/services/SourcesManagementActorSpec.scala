package net.truerss.services

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.{AfterAll, Scope}
import truerss.api.{BadRequestResponse, ModelResponse, WSController}
import truerss.db.{DbLayer, SourceDao}
import truerss.dto.NewSourceDto
import truerss.services.SourcesActor
import truerss.services.actors.SourcesManagementActor.AddSource
import truerss.services.actors.{DtoModelImplicits, SourcesManagementActor}
import truerss.util.ApplicationPlugins

import scala.concurrent.Future
import scala.concurrent.duration._

class SourcesManagementActorSpec extends TestKit(ActorSystem("addSourceSpec"))
 with SpecificationLike with AfterAll with Mockito {

  sequential
/*
  import DtoModelImplicits._

  val duration = 10 seconds

  val name1 = "name1"
  val name2 = "name2"
  val url1 = "http://example1.com"
  val url2 = "http://example2.com"

  val source1Dto = Gen.genNewSource.copy(
    name = name1, url = url1
  )
  val source2Dto = Gen.genNewSource.copy(
    name = name2, url = url2
  )

  val plugins = ApplicationPlugins()


  "sources management actor" should {
    "add new source" in new TestScope(0, 0, source1Dto) {
      val source1 = source1Dto.toSource
      service.expectMsg(WSController.SourceAdded(source1.withId(id)))
      service.expectMsg(SourcesActor.NewSource(source1.withId(id)))

      me.expectMsg(ModelResponse(source1.withId(id)))
    }

    "produce error when url and name is not valid" in new TestScope(1, 1, source2Dto) {
      service.expectNoMessage(duration)
      me.expectMsgClass(classOf[BadRequestResponse])
    }
  }

  class TestScope(nameCount: Int, urlCount: Int, dto: NewSourceDto) extends Scope {
    val id = 1L
    val db = mock[DbLayer]
    val mockedSourceDao = mock[SourceDao]
    db.sourceDao returns mockedSourceDao
    mockedSourceDao.findByName(dto.name, None) returns Future.successful(nameCount)
    mockedSourceDao.findByUrl(dto.url, None) returns Future.successful(urlCount)
    val source = dto.toSource
    mockedSourceDao.insert(source) returns Future.successful(id)

    val ref = TestActorRef(new SourcesManagementActor(db, plugins))
    val me = TestProbe()
    val service = TestProbe()
    system.eventStream.subscribe(service.ref, classOf[WSController.SourceAdded])
    system.eventStream.subscribe(service.ref, classOf[SourcesActor.NewSource])

    val msg = AddSource(dto)

    ref.tell(msg, me.ref)
  }

*/
  override def afterAll(): Unit = {
    system.terminate()
  }

}
