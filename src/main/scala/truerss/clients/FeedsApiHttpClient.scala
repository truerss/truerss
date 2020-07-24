package truerss.clients

import truerss.api.JsonFormats
import truerss.dto._
import zio.Task

class FeedsApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val feedsUrl = s"$baseUrl/$api/feeds"

  def favorites(offset: Int, limit: Int): Task[Page[FeedDto]] = {
    get[Page[FeedDto]](s"$feedsUrl/favorites")
  }

  def findOne(feedId: Long): Task[FeedDto] = {
    get[FeedDto](s"$feedsUrl/$feedId")
  }

  def content(feedId: Long): Task[FeedContent] = {
    get[FeedContent](s"$feedsUrl/content/$feedId")
  }

  def mark(feedId: Long): Task[Unit] = {
    put[Unit](s"$feedsUrl/mark/$feedId")
  }

  def unmark(feedId: Long): Task[Unit] = {
    put[Unit](s"$feedsUrl/unmark/$feedId")
  }

  def read(feedId: Long): Task[Unit] = {
    put[Unit](s"$feedsUrl/read/$feedId")
  }

  def unread(feedId: Long): Task[Unit] = {
    put[Unit](s"$feedsUrl/unread/$feedId")
  }

}
