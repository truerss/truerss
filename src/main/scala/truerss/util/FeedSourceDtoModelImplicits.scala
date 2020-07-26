package truerss.util

import java.time.{Clock, LocalDateTime}

import truerss.db.{Feed, Source, SourceStates}
import truerss.dto.{FeedDto, NewSourceDto, SourceViewDto, UpdateSourceDto}

object FeedSourceDtoModelImplicits {

  import CommonImplicits._

  implicit class NewSourceDtoExt(val x: NewSourceDto) extends AnyVal {
    def toSource: Source = {
      Source(
        id = None,
        url = x.url,
        name = x.name,
        interval = x.interval,
        state = SourceStates.Neutral,
        normalized = x.name.normalize,
        lastUpdate = LocalDateTime.now(Clock.systemUTC())
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
        state = SourceStates.Neutral,
        normalized = x.name.normalize,
        lastUpdate = LocalDateTime.now(Clock.systemUTC())
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

  implicit class FeedExt(val x: Feed) extends AnyVal {
    def toDto: FeedDto = {
      FeedDto(
        id = x.id.getOrElse(0L),
        sourceId = x.sourceId,
        url = x.url,
        title = x.title,
        author = x.author,
        publishedDate = x.publishedDate,
        description = x.description,
        content = x.content,
        normalized = x.normalized,
        favorite = x.favorite,
        read = x.read,
        delete = x.delete
      )
    }
  }

}
