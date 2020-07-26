package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import truerss.api.{HttpApi, OpmlApi}
import truerss.dto.SourceViewDto
import truerss.services.OpmlService
import zio.Task

class OpmlApiTests extends BaseApiTest {

  private val path = "opml"

  private val opmlContent = "<test></test>"

  "opml api" should {
    "download current opml" in new Test() {
      Get(api(s"$path")) ~> route ~> check {
        mediaType ==== HttpApi.opml.mediaType
        status ==== StatusCodes.OK
      }
    }

    "#reprocess" in {
      OpmlApi.reprocessToOpml(entity) ==== opml.split("\n").map(_.trim).mkString("\n")
    }
  }

  private class Test() extends BaseScope {
    val service = new OpmlService(null) {
      override def build: Task[String] = Task.succeed(opmlContent)

      override def create(text: String): Task[Iterable[SourceViewDto]] = ???
    }
    override protected val route: Route = new OpmlApi(service).route
  }

  private def entity =
    s"""
      |--------------------------d078de50dc3eca6f
      |Content-Disposition: form-data; name="import"; filename="test.opml"
      |Content-Type: application/octet-stream
      |
      |$opml
      |
      |--------------------------d078de50dc3eca6f""".stripMargin

  private def opml =
    """|<?xml version="1.0"?>
      |<opml version="1.0">
      |<head>TrueRSS Feed List Export</head>
      |<body>
      |<outline title="Newsfeeds exported from Truerss" text="Newsfeeds exported from Truerss" description="Newsfeeds exported from Truerss" type="folder">
      |    <outline type="rss" text="update-test" title="test" xmlUrl="http://%%HOST%%:%%PORT%%/rss"></outline>
      |    <outline type="rss" text="test#2" title="test" xmlUrl="http://%%HOST%%:%%PORT%%/rss1"></outline>
      |    <outline type="rss" text="update-test" title="test" xmlUrl="http://%%HOST%%:%%PORT%%/rss2"></outline>
      |    <outline type="rss" text="update-test" title="test" xmlUrl="http://%%HOST%%:%%PORT%%/boom"></outline>
      |</outline>
      |</body>
      |</opml>""".stripMargin

}
