package truerss.api

import truerss.services.{FeedsService, SourcesService}
import truerss.util.CommonImplicits
import com.github.fntz.omhs.{BodyReader, BodyWriter, ParamDSL}
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.playjson.JsonSupport
import truerss.dto.{FeedDto, NewSourceDto, Page, SourceViewDto, UpdateSourceDto}

class SourcesApi(feedsService: FeedsService,
                 sourcesService: SourcesService
                ) extends HttpApi {

  import QueryPage._
  import JsonFormats._
  import CommonImplicits.StringExt
  import ZIOSupport._
  import ParamDSL._
  import RoutingImplicits._
  import SourceFeedsFilter._

  implicit val pageFeedDtoWriter: BodyWriter[Page[FeedDto]] =
    JsonSupport.writer[Page[FeedDto]]

  implicit val sourceVViewWriter: BodyWriter[Vector[SourceViewDto]] =
    JsonSupport.writer[Vector[SourceViewDto]]

  implicit val sourceViewWriter: BodyWriter[SourceViewDto] =
    JsonSupport.writer[SourceViewDto]

  implicit val newSourceDtoReader: BodyReader[NewSourceDto] =
    JsonSupport.reader[NewSourceDto]

  implicit val updateSourceDtoReader: BodyReader[UpdateSourceDto] =
    JsonSupport.reader[UpdateSourceDto]

  // just aliases
  private val fs = feedsService
  private val ss = sourcesService

  private val base = "api" / "v1" / "sources"
  private val all = get(base / "all") ~> { () =>
    ss.findAll
  }
  private val findOne = get(base / long) ~> { (sourceId: Long) =>
    ss.getSource(sourceId)
  }

  // todo to feeds api
  private val latest = get(base / "latest" / query[QueryPage]) ~> { (q: QueryPage) =>
    fs.latest(q.offset, q.limit)
  }

  private val feeds = get(base / long / "feeds" / query[SourceFeedsFilter]) ~> { (sourceId: Long, f: SourceFeedsFilter) =>
    fs.findBySource(sourceId, f.unreadOnly, f.offset, f.limit)
  }

  private val newSource = post(base / body[NewSourceDto]) ~> { (newSource: NewSourceDto) =>
    ss.addSource(newSource)
  }

  private val updateSource = put(base / long / body[UpdateSourceDto]) ~> { (sourceId: Long, dto: UpdateSourceDto) =>
    ss.updateSource(sourceId, dto)
  }

  private val deleteSource = delete(base / long) ~> { (sourceId: Long) =>
    ss.delete(sourceId)
  }

  val route = ???


}