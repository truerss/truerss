package truerss.api

import com.github.fntz.omhs.{BodyWriter, ParamDSL, QueryReader}
import truerss.dto.{FeedContent, FeedDto, Page}
import truerss.services.{ContentReaderService, FeedsService}
import truerss.util.CommonImplicits
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.playjson.JsonSupport

class FeedsApi(feedsService: FeedsService,
               contentReaderService: ContentReaderService
              ) extends HttpApi {

  import QueryPage._
  import JsonFormats._
  import RoutingImplicits._
  import ParamDSL._
  import ZIOSupport._

  private val fs = feedsService
  private val crs = contentReaderService

  implicit val pageWriter: BodyWriter[Page[FeedDto]] = JsonSupport.writer[Page[FeedDto]]
  implicit val feedDtoWriter: BodyWriter[FeedDto] = JsonSupport.writer[FeedDto]
  implicit val feedContentWriter: BodyWriter[FeedContent] = JsonSupport.writer[FeedContent]

  private val base = "api" / "v1" / "feeds"

  private val favorites = get(base / "favorites" / query[QueryPage]) ~> { (q: QueryPage) =>
    fs.favorites(q.offset, q.limit)
  }

  private val findOne = get(base / long) ~> { (feedId: Long) =>
    fs.findOne(feedId)
  }

  private val findContent = get(base / "content" / long) ~> { (feedId: Long) =>
    crs.fetchFeedContent(feedId)
  }

  private val markFeed = put(base / "mark" / long) ~> { (feedId: Long) =>
    fs.changeFav(feedId, favFlag = true)
  }

  private val unmarkFeed = put(base / "unmark" / long) ~> { (feedId: Long) =>
    fs.changeFav(feedId, favFlag = false)
  }

  private val readFeed = put(base / "read" / long) ~> { (feedId: Long) =>
    fs.changeRead(feedId, readFlag = true)
  }

  private val unreadFeed = put(base / "unread" / long) ~> { (feedId: Long) =>
    fs.changeRead(feedId, readFlag = false)
  }

  val route = favorites :: findOne :: findContent :: markFeed :: unmarkFeed :: readFeed :: unmarkFeed

}
