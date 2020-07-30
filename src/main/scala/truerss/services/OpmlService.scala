package truerss.services

import truerss.dto.{NewSourceDto, SourceViewDto}
import truerss.services.OpmlService.fromOutlines
import truerss.util.{OpmlBuilder, OpmlParser, Outline}
import zio.Task

class OpmlService(private val sourcesService: SourcesService) {

  def build: Task[String] = {
    sourcesService.getAllForOpml.map(OpmlBuilder.build)
  }

  def create(text: String): Task[Unit] = {
    for {
      xs <- OpmlParser.parse(text)
      fs = fromOutlines(xs)
      result <- sourcesService.addSources(fs)
    } yield result
  }

}

object OpmlService {
  val interval = 8

  def from(url: String, name: String, interval: Int): NewSourceDto = {
    NewSourceDto(
      url = url,
      name = name,
      interval = interval
    )
  }

  def fromOutlines(xs: Iterable[Outline]): Iterable[NewSourceDto] = {
    xs.map { x =>
      from(x.link, x.title, interval)
    }
  }
}