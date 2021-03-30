package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.services.{ContentReaderService, FeedsService}

class FeedsApi(feedsService: FeedsService,
               contentReaderService: ContentReaderService
              ) {

  import OMHSSupport._
  import QueryPage._
  import RoutingDSL._
  import ZIOSupport._

  private val fs = feedsService
  private val crs = contentReaderService

  private val base = "api" / "v1" / "feeds"

  private val favorites = get(base / "favorites" :? query[QueryPage]) ~> { (q: QueryPage) =>
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

  val route = favorites :: findOne :: findContent :: markFeed ::
    unmarkFeed :: readFeed :: unreadFeed

}
