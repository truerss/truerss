package net.truerss.services

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.services.{OpmlService, SourcesService}
import truerss.util.Outline
import net.truerss.FutureTestExt
import truerss.dto.NewSourceDto
import truerss.services.actors.sync.SourcesKeeperActor

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.xml.XML

class OpmlServiceTest(implicit ee: ExecutionEnv) extends Specification with Mockito {

  import FutureTestExt._

  private val duration = 10 seconds

  private val sourceService = mock[SourcesService]
  private val es = mock[EventStream]
  private val v = Gen.genView

  val v1 = Gen.genView
  val v2 = Gen.genView
  sourceService.getAllForOpml.returns(Future.successful(Vector(v)))
  sourceService.addSources(any[Iterable[NewSourceDto]]) returns(Future.successful(
    Iterable(v1, v2)
  ))

  private val opmlService = new OpmlService(sourceService, es)


  "OpmlService" should {
    "produce opml" in {
      opmlService.build.map { x =>
        x must contain(v.name)
        x must contain(v.url)
        val outlines = XML.loadString(x) \\ "outline"
        outlines.size ==== 2 // one for folder, one for source
      }.await
    }

    "parse text: normal" in {
      val text = Await.result(opmlService.build, duration)
      val result = opmlService.create(text)
      result ~> { _ must beRight }

      there was one(sourceService)
    }

    "parse text: invalid" in {
      opmlService.create("boom") ~> { _ must beLeft }
    }
  }

}
