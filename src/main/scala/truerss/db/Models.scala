package truerss.db

import java.util.Date

import truerss.dto.NewSourceDto
import truerss.util.Util._

import scala.language.postfixOps

sealed trait SourceState {
  val number: Int
}

object SourceStates {
  case object Neutral extends SourceState {
    override val number: Int = 0
  }
  case object Enable extends SourceState {
    override val number: Int = 1
  }
  case object Disable extends SourceState {
    override val number: Int = 2
  }
}

case class Source(id: Option[Long],
                  url: String,
                  name: String,
                  interval: Int,
                  state: SourceState,
                  normalized: String,
                  lastUpdate: Date,
                  count: Int = 0) {

  def normalize: Source = copy(normalized = name.normalize)
  def recount(x: Int): Source = copy(count = x)
  def withId(x: Long): Source = copy(id = Some(x))
  def withState(x: SourceState): Source = copy(state = state)

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
                delete: Boolean = false) {
  def mark(flag: Boolean): Feed = copy(favorite = flag)
}



