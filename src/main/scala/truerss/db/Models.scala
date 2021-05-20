package truerss.db

import java.time.LocalDateTime

import truerss.util.CommonImplicits._

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
                  lastUpdate: LocalDateTime,
                  count: Int = 0) {

  def normalize: Source = copy(normalized = name.normalize)
  def withId(x: Long): Source = copy(id = Some(x))
  def withState(x: SourceState): Source = copy(state = x)

}


case class Feed(id: Option[Long],
                sourceId: Long,
                url: String,
                title: String,
                author: String,
                publishedDate: LocalDateTime,
                description: Option[String],
                content: Option[String],
                normalized: String,
                favorite: Boolean = false,
                read: Boolean = false,
                delete: Boolean = false) {
  def mark(flag: Boolean): Feed = copy(favorite = flag)
}

case class Version(id: Long, fact: String, when: LocalDateTime)

