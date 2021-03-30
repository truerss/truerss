package truerss.api

import com.github.fntz.omhs.QueryReader
import truerss.util.CommonImplicits

case class QueryPage(offset: Int, limit: Int)
object QueryPage {
  import CommonImplicits._

  def first(queries: Map[String, Iterable[String]], name: String): Option[String] = {
    queries.get(name).flatMap(_.headOption)
  }

  def offset(queries: Map[String, Iterable[String]]): Int = {
    first(queries, "offset").map(_.toIntOr(0)).getOrElse(0)
  }

  def limit(queries: Map[String, Iterable[String]]): Int = {
    first(queries, "limit").map(_.toIntOr(100)).getOrElse(100)
  }

  implicit val queryReader: QueryReader[QueryPage] = (queries: Map[String, Iterable[String]]) => {
    Some(QueryPage(offset(queries), limit(queries)))
  }
}
case class SourceFeedsFilter(unreadOnly: Boolean, offset: Int, limit: Int)
object SourceFeedsFilter {
  import QueryPage._

  implicit val sourceFeedsFilterReader: QueryReader[SourceFeedsFilter] = new QueryReader[SourceFeedsFilter] {
    override def read(queries: Map[String, Iterable[String]]): Option[SourceFeedsFilter] = {
      val unreadOnly = first(queries, "unreadOnly").forall(_.toBoolean)
      Some(SourceFeedsFilter(
        unreadOnly = unreadOnly,
        offset = offset(queries),
        limit = limit(queries)
      ))
    }
  }
}