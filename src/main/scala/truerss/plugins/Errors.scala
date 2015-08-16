package truerss.plugins

object Errors {
  sealed trait Error { val error: String }
  case class ParsingError(error: String) extends Error
  case class ConnectionError(error: String) extends Error
  case class UnexpectedError(error: String) extends Error
}
