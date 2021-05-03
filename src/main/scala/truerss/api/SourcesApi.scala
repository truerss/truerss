package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.{FeedsService, SourcesService}

class SourcesApi(feedsService: FeedsService,
                 sourcesService: SourcesService
                ) {

  import OMHSSupport._
  import QueryPage._
  import RoutingDSL._
  import SourceFeedsFilter._
  import ZIOSupport._

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
  private val latest = get(base / "latest" :? query[QueryPage]) ~> { (q: QueryPage) =>
    fs.latest(q.offset, q.limit)
  }

  private val feeds = get(base / long / "feeds" :? query[SourceFeedsFilter]) ~> { (sourceId: Long, f: SourceFeedsFilter) =>
    fs.findBySource(sourceId, f.unreadOnly, f.offset, f.limit)
  }

  private val newSource = post(base <<< body[NewSourceDto]) ~> { (newSource: NewSourceDto) =>
    ss.addSource(newSource)
  }

  private val updateSource = put(base / long <<< body[UpdateSourceDto]) ~> { (sourceId: Long, dto: UpdateSourceDto) =>
    ss.updateSource(sourceId, dto)
  }

  private val deleteSource = delete(base / long) ~> { (sourceId: Long) =>
    ss.delete(sourceId)
  }

  val route = all :: findOne :: latest :: feeds :: newSource :: updateSource :: deleteSource


}