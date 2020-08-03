package truerss.services

case class ValidationError(errors: List[String]) extends Throwable

object ValidationError {
  def apply(error: String): ValidationError = new ValidationError(error :: Nil)
}

case class ContentReadError(error: String) extends Throwable

case class NotFoundError(entityId: Long) extends Throwable