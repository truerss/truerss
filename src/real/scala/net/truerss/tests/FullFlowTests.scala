package net.truerss.tests

import net.truerss.{Gen, Resources, ZIOMaterializer}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import play.api.libs.json._
import truerss.api.ws.WebSocketController.NewFeeds
import truerss.clients.{EntityNotFoundError, _}
import truerss.db.Predefined
import truerss.dto._
import zio.Task

trait FullFlowTests extends Specification with Resources with BeforeAfterAll {

  import ZIOMaterializer._
  import WSReaders._

  private val sourceApiClient = new SourcesApiHttpClient(url)
  private val feedsApiClient = new FeedsApiHttpClient(url)
  private val refreshApiClient = new RefreshApiHttpClient(url)
  private val markApiClient = new MarkApiHttpClient(url)
  private val overviewApiClient = new SourcesOverviewApiHttpClient(url)
  private val searchApiClient = new SearchApiHttpClient(url)
  private val opmlApiClient = new OpmlApiHttpClient(url)
  private val settingsClient = new SettingsApiHttpClient(url)

  "APIs" should {
    "scenario" in {
      // check ws
      wsClient.isOpen must beTrue

      // create an source
      val newSource = NewSourceDto(
        url = rssUrl,
        name = "test#1",
        interval = 1
      )

      val createdSource = sourceApiClient.create(newSource).m

      val sourceId = createdSource.id

      p(s"Create Source: $sourceId -> ${newSource.url}")

      val getSource = sourceApiClient.findOne(sourceId).m

      getSource.url ==== newSource.url
      getSource.name ==== newSource.name
      getSource.interval ==== newSource.interval

      p("Try to create with the same name")
      val newSourceInvalidName = newSource.copy(url = "http://example.com/rss")
      sourceApiClient.create(newSourceInvalidName)
        .err[BadRequestError].errors ==== Iterable(s"Name '${newSource.name}' is not unique")

      p("Try to create with the same url")
      val newSourceInvalidUrl = newSource.copy(name = Gen.genUrl)
      sourceApiClient.create(newSourceInvalidUrl)
        .err[BadRequestError].errors ==== Iterable(s"Url '${newSource.url}' is not unique")

      p("Try to create with invalid interval")
      val newSourceInvalidInterval = newSource.copy(interval = -1)
      sourceApiClient.create(newSourceInvalidInterval)
        .err[BadRequestError].errors ==== Iterable("Interval must be great than 0")

      p("Try to create with not rss url")
      val newSourceWithNotRssUrl = newSource.copy(name = "test#2", url = s"http://$host:$serverPort/not-rss")
      sourceApiClient.create(newSourceWithNotRssUrl)
        .err[BadRequestError].errors ==== Iterable(s"${newSourceWithNotRssUrl.url} is not a valid RSS/Atom feed")

      p("Try to get not existing source")
      sourceApiClient.findOne(1000).e must beLeft(EntityNotFoundError)

      p("Try to get all sources")
      sourceApiClient.findAll.m must have size 1

      p("Try to update source")
      val updateSource = UpdateSourceDto(
        id = sourceId,
        url = newSource.url,
        name = "updated-source",
        interval = 10
      )
      val updated = sourceApiClient.update(sourceId, updateSource).m
      updated.url ==== updateSource.url
      updated.name ==== updateSource.name
      updated.interval ==== updateSource.interval
      updated.id ==== updateSource.id

      val dto = sourceApiClient.findOne(sourceId).m
      dto.url ==== updateSource.url
      dto.name ==== updateSource.name
      dto.interval ==== updateSource.interval
      dto.id ==== updateSource.id

      wsClient.newFeeds must have size 1
      val newFeeds1 = wsClient.newFeeds.head
      newFeeds1 must have size 1
      newFeeds1.foreach {_.sourceId ==== sourceId }

      p("Refresh One")

      refreshApiClient.refreshOne(sourceId).e must beRight

      sleep()

      wsClient.newFeeds.size ==== 1

      p("Refresh All")
      refreshApiClient.refreshAll.e must beRight

      sleep()

      getRssStats must beGreaterThanOrEqualTo(4) // we sent a few requests before (when tried to create source)

      sourceApiClient.findOne(sourceId).m.lastUpdate !== dto.lastUpdate

      p("Get Overview")
      val overview = overviewApiClient.overview(sourceId).m

      // one unread feed @see testServer/rss
      overview.unreadCount ==== 1
      overview.feedsCount ==== 1
      overview.favoritesCount ==== 0

      p("Get Overview for not existing source")
      overviewApiClient.overview(1000).e must beLeft(EntityNotFoundError)

      sourceApiClient.unread(sourceId).m must have size 1
      val latest = sourceApiClient.latest(0, 100).m
      latest.total ==== 1
      latest.resources must have size 1

      sourceApiClient.latest(1, 100).m.resources must have size 0

      val favorites = sourceApiClient.feeds(sourceId, unreadOnly = false, 0, 100).m
      favorites.total ==== 1
      favorites.resources must have size 1

      sourceApiClient.feeds(sourceId, unreadOnly = true, 0, 100).m.resources must have size 1

      val feed = latest.resources.head
      val feedId = feed.id
      feed.content must beNone
      feed.read must beFalse
      feed.favorite must beFalse

      // feeds api
      feedsApiClient.findOne(feedId).m ==== feed

      feedsApiClient.content(feedId).m.content.map(clear) must beSome(content)

      feedsApiClient.read(feedId).m

      sourceApiClient.feeds(sourceId, unreadOnly = true, 0, 100).m.resources must be empty

      sourceApiClient.unread(sourceId).m must be empty

      feedsApiClient.findOne(feedId).m.read must beTrue

      feedsApiClient.unread(feedId).m

      feedsApiClient.findOne(feedId).m.read must beFalse

      feedsApiClient.mark(feedId).m

      feedsApiClient.findOne(feedId).m.favorite must beTrue

      val currentFavorites = feedsApiClient.favorites(0, 100).m
      currentFavorites.total ==== 1
      currentFavorites.resources.map(_.id) must contain(exactly(feedId))

      feedsApiClient.unmark(feedId).m

      feedsApiClient.findOne(feedId).m.favorite must beFalse

      // add another source
      val source2 = newSource.copy(url = rssUrl1, name = "test#2")
      val sourceId2 = sourceApiClient.create(source2).m.id

      sleep() // waiting for sync source2

      // produce new entities
      produceNewEntities

      refreshApiClient.refreshOne(sourceId).m

      sleep() // waiting for sync source1

      feedsApiClient.read(feedId).m

      // because we do not sink yet
      sourceApiClient.feeds(sourceId2, unreadOnly = false, 0, 100).m.resources must have size 1

      // markApi
      val unreadInSource1 = sourceApiClient.unread(sourceId).m
      unreadInSource1 must have size 2
      unreadInSource1.map(_.id).filter(_ == feedId) must be empty

      markApiClient.markOne(sourceId).m

      sourceApiClient.unread(sourceId).m must be empty

      // we did not sync
      sourceApiClient.unread(sourceId2).m must have size 1

      refreshApiClient.refreshAll.m

      sleep() // wait for sync

      sourceApiClient.unread(sourceId2).m must have size 3

      markApiClient.markAll.m

      sourceApiClient.unread(sourceId2).m must be empty

      // search
      searchApiClient.search(SearchRequest(
        inFavorites = false,
        query = "test#1",
        offset = 0,
        limit = 100
      )).m.resources must have size 2

      searchApiClient.search(SearchRequest(
        inFavorites = true,
        query = "test#1",
        offset = 0,
        limit = 100
      )).m.resources must have size 0

      feedsApiClient.mark(feedId).m

      p("SearchApi")
      searchApiClient.search(SearchRequest(
        inFavorites = false,
        query = "test#1",
        offset = 0,
        limit = 1
      )).m.total ==== 2

      // pagination check
      searchApiClient.search(SearchRequest(
        inFavorites = true,
        query = "test#1",
        offset = 0,
        limit = 1
      )).m.total ==== 1

      p("Read where feed content is not available")
      val feedId2 = searchApiClient.search("boom").m.resources.head.id

      feedsApiClient.content(feedId2).err[BadRequestError].errors.head must contain("Connection error")

      p("Opml Builders")
      val opml = opmlApiClient.download.m

      opml must contain(updateSource.name)
      opml must contain(updateSource.url)
      opml must contain(source2.url)
      opml must contain(source2.url)

      p("Import sources")
      val result = opmlApiClient.importFile(opmlFile).m

      result must have size 1

      // add one more new source
      val newSource3 = NewSourceDto(
        url = rssUrlWithError,
        name = "test-source-3",
        interval = 1
      )

      val sourceDto3 = sourceApiClient.create(newSource3).m
      sourceDto3.interval ==== newSource3.interval
      sourceDto3.url ==== newSource3.url
      sourceDto3.name ==== newSource3.name

      val sourceId3 = sourceDto3.id

      sleep() // waiting for updates

      wsClient.notifications must be empty // still empty

      wsClient.newFeeds.last.foreach { _.sourceId ==== sourceId3 } // and we have feeds from source3

      produceErrors      // generate errors in source3

      refreshApiClient.refreshOne(sourceId3).m

      sleep()

      wsClient.notifications.size ==== 1

      val notification = wsClient.notifications.head
      notification.level ==== NotifyLevel.Danger
      notification.message must contain(s"Connection error for $rssUrlWithError")

      // delete source
      sourceApiClient.deleteOne(sourceId).m

      sourceApiClient.findOne(sourceId).e must beLeft(EntityNotFoundError)
      feedsApiClient.findOne(feedId).e must beLeft(EntityNotFoundError)
      sourceApiClient.unread(sourceId).m must be empty

      success
    }
  }

  private def clear(x: String): String = {
    x
      .trim
      .stripMargin
      .replaceAll("\n", "")
      .replaceAll(" ", "")
  }

  private def p(x: String) = {
    println(s"==========> $x")
  }

}
