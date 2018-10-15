package truerss.services.actors

import java.util.Date

import truerss.dto.{NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.models.{Neutral, Source}
import truerss.util.Util._

object DtoModelImplicits {

  implicit class NewSourceDtoExt(val x: NewSourceDto) extends AnyVal {
    def toSource: Source = {
      Source(
        id = None,
        url = x.url,
        name = x.name,
        interval = x.interval,
        state = Neutral,
        normalized = x.name.normalize,
        lastUpdate = new Date()
      )
    }
  }

  implicit class UpdateSourceDtoExt(val x: UpdateSourceDto) extends AnyVal {
    def toSource: Source = {
      Source(
        id = Some(x.id),
        url = x.url,
        name = x.name,
        interval = x.interval,
        state = Neutral,
        normalized = x.name.normalize,
        lastUpdate = new Date()
      )
    }
  }

  implicit class SourceExt(val x: Source) extends AnyVal {
    def toView: SourceViewDto = {
      SourceViewDto(
        id = x.id.getOrElse(0L),
        url = x.url,
        name = x.name,
        interval = x.interval,
        state = x.state,
        normalized = x.normalized,
        lastUpdate = x.lastUpdate,
        count = x.count
      )
    }
  }

}
