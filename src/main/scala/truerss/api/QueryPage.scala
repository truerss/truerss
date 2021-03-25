package truerss.api

import com.github.fntz.omhs.QueryReader
import truerss.util.CommonImplicits

case class QueryPage(offset: Int, limit: Int)
object QueryPage {
  import CommonImplicits.StringExt

  def first(queries: Map[String, Iterable[String]], name: String): Option[String] = {
    queries.get(name).flatMap(_.headOption)
  }

  implicit val queryReader: QueryReader[QueryPage] = (queries: Map[String, Iterable[String]]) => {
    // todo
    val offset = first(queries, "offset").map(_.toIntOr(0)).getOrElse(0)
    val limit = first(queries, "limit").map(_.toIntOr(100)).getOrElse(100)
    Some(QueryPage(offset, limit))
  }
}
case class SourceFeedsFilter(unreadOnly: Boolean, offset: Int, limit: Int)
object SourceFeedsFilter {
  import CommonImplicits.StringExt
  import QueryPage.first

  implicit val sourceFeedsFilterReader: QueryReader[SourceFeedsFilter] = new QueryReader[SourceFeedsFilter] {
    override def read(queries: Map[String, Iterable[String]]): Option[SourceFeedsFilter] = {
      val offset = first(queries, "offset").map(_.toIntOr(0)).getOrElse(0)
      val limit = first(queries, "limit").map(_.toIntOr(100)).getOrElse(100)
      val unreadOnly = first(queries, "unreadOnly").forall(_.toBoolean)
      Some(SourceFeedsFilter(
        unreadOnly = unreadOnly,
        offset = offset,
        limit = limit
      ))
    }
  }
}