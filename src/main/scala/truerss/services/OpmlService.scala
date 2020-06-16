package truerss.services

import truerss.util.{OpmlBuilder, OpmlParser, Outline}

import scala.concurrent.{ExecutionContext, Future}

class OpmlService(sourcesService: SourcesService)(implicit ec: ExecutionContext) {

  import truerss.util.syntax.\/

  def build: Future[String] = {
    sourcesService.getAllForOpml.map(OpmlBuilder.build)
  }

  def parse(text: String): String \/ Iterable[Outline] = {
    OpmlParser.parse(text)
  }

}
