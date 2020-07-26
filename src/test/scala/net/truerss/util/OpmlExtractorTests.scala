package net.truerss.util

import org.specs2.mutable.Specification
import truerss.util.OpmlExtractor

class OpmlExtractorTests extends Specification {

  "opml extractor" should {
    "#reprocess" in {
      OpmlExtractor.reprocessToOpml(entity) ==== opml.split("\n").map(_.trim).mkString("\n")
    }
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
