package truerss.util

import truerss.dto.SourceViewDto

import scala.xml.Utility

object OpmlBuilder {
  // util variables
  private val exportText = "Newsfeeds exported from TrueRSS"

  def build(sources: Seq[SourceViewDto]): String = {
    val outlines = sources.map { source =>
      s"""<outline type="rss" text="${e(source.name)}" title="${e(source.name)}" xmlUrl="${e(source.url)}"></outline>"""
    }.mkString("\n")
      s"""|<?xml version="1.0"?>
          |<opml version="1.0">
          |<head>TrueRSS Feed List Export</head>
          |<body>
          |<outline title="$exportText" text="$exportText" description="$exportText" type="folder">
          |$outlines
          |</outline>
          |</body>
          |</opml>
       """.stripMargin
  }

  private def e(x: String): String = Utility.escape(x)

}
