package truerss.models

import java.util.Date

import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption._
import truerss.util.Util._
import truerss.util.Jsonize

import scala.language.postfixOps

sealed trait SourceState
case object Neutral extends SourceState
case object Enable extends SourceState
case object Disable extends SourceState

case class SourceW(id: Option[Long],
              url: String,
              name: String,
              interval: Int
             ) {
  def toSource = {
    Source(
      id = id,
      url = url,
      name = name,
      interval = interval,
      state = Neutral,
      normalized = name.normalize,
      lastUpdate = new Date()
    )
  }
}



case class Source(id: Option[Long],
                  url: String,
                  name: String,
                  interval: Int,
                  state: SourceState,
                  normalized: String,
                  lastUpdate: Date,
                  count: Int = 0) extends Jsonize {

  def normalize: Source = copy(normalized = name.normalize)
  def recount(x: Int): Source = copy(count = x)
  def withId(x: Long): Source = copy(id = Some(x))
  def withState(x: SourceState): Source = copy(state = state)

}

object SourceHelper {
  def from(url: String, name: String, interval: Int): Source = {
    Source(
      id = None,
      url = url,
      name = name,
      interval = interval,
      state = Neutral,
      normalized = name,
      lastUpdate = new Date()
    )
  }
}

case class Feed(id: Option[Long],
                sourceId: Long,
                url: String,
                title: String,
                author: String,
                publishedDate: Date,
                description: Option[String],
                content: Option[String],
                normalized: String,
                favorite: Boolean = false,
                read: Boolean = false,
                delete: Boolean = false) extends Jsonize {
  def mark(flag: Boolean): Feed = copy(favorite = flag)
}

case class WSMessage(messageType: String, body: String)

