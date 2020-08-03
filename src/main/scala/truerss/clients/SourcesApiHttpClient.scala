package truerss.clients

import play.api.libs.json.Json
import truerss.api.JsonFormats
import truerss.dto._
import zio.Task

class SourcesApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val sourcesUrl = s"$baseUrl/$api/sources"

  def findAll: Task[Iterable[SourceViewDto]] = {
    get[Iterable[SourceViewDto]](s"$sourcesUrl/all")
  }

  def findOne(sourceId: Long): Task[SourceViewDto] = {
    get[SourceViewDto](s"$sourcesUrl/$sourceId")
  }

  def deleteOne(sourceId: Long): Task[Unit] = {
    delete[Unit](s"$sourcesUrl/$sourceId")
  }

  def create(newSource: NewSourceDto): Task[SourceViewDto] = {
    post[SourceViewDto](sourcesUrl, Json.toJson(newSource).toString())
  }

  def update(sourceId: Long, source: UpdateSourceDto): Task[SourceViewDto] = {
    put[SourceViewDto](s"$sourcesUrl/$sourceId", Json.toJson(source).toString())
  }

  def latest(offset: Int, limit: Int): Task[Page[FeedDto]] = {
    get[Page[FeedDto]](s"$sourcesUrl/latest?offset=$offset&limit=$limit")
  }

  def feeds(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Task[Page[FeedDto]] = {
    get[Page[FeedDto]](s"$sourcesUrl/$sourceId/feeds?unreadOnly=$unreadOnly&offset=$offset&limit=$limit")
  }

  def unread(sourceId: Long, offset: Int, limit: Int): Task[Page[FeedDto]] = {
    feeds(sourceId, unreadOnly = true, offset, limit)
  }

}
