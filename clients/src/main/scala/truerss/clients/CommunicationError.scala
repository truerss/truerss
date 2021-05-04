package truerss.clients

sealed trait ClientError extends Throwable {
  def code: Int
}

sealed trait ReasonableError extends ClientError

case class BadRequestError(errors: Iterable[String]) extends ReasonableError {
  override def code: Int = 404
}

object BadRequestError {
  def apply(error: String): BadRequestError = new BadRequestError(error :: Nil)
}

case class ContentReadError(error: String) extends ReasonableError {
  override def code: Int = 500
}

case object EntityNotFoundError extends ClientError {
  override def code: Int = 404
}

case class UnexpectedError(error: String, code: Int) extends ClientError