package net.truerss.services

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.services.{OpmlService, SourcesService}

import scala.concurrent.Future
import scala.xml.XML

class OpmlServiceTest(implicit ee: ExecutionEnv) extends Specification with Mockito {

  private val sourceService = mock[SourcesService]
  private val v = Gen.genView

  sourceService.getAllForOpml.returns(Future.successful(Vector(v)))

  private val opmlService = new OpmlService(sourceService)


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
      opmlService.build.map { x =>
        val result = opmlService.parse(x)
        result must beRight
        val xs = result.toOption.getOrElse(Iterable.empty)
        xs must have size 1
        val h = xs.head
        h.link ==== v.url
        h.title ==== v.name
      }.await
    }

    "parse text: invalid" in {
      val result = opmlService.parse("boom")
      result must beLeft
    }
  }

}
