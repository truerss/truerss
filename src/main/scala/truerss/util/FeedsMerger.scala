package truerss.util

import com.github.truerss.base.Entry
import truerss.db.Feed

object FeedsMerger {
  import EntryImplicits._

  def calculate(sourceId: Long, xs: Iterable[Entry], inDb: Iterable[Feed]): FeedCalc = {
    val inDbMap = inDb.map(f => f.url -> f).toMap
    val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)

    val (feedsToUpdateByUrl, feedsToInsert) = forceUpdateXs
      .map(_.toFeed(sourceId))
      .partition(f => inDbMap.contains(f.url))

    val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
    val inDbUrls = inDbMap.keySet
    val fromNetwork = updateXsMap.keySet
    val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
    FeedCalc(
      feedsToUpdateByUrl = feedsToUpdateByUrl,
      feedsToInsert = (feedsToInsert ++ newFeeds).groupBy(_.url).values.flatten
    )
  }
}


case class FeedCalc(
                     feedsToUpdateByUrl: Iterable[Feed],
                     feedsToInsert: Iterable[Feed]
                   )