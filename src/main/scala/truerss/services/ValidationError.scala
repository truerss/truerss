package truerss.services

case class ValidationError(errors: List[String]) // extends Throwable

case class ContentReadError(error: String) extends Throwable

case class NotFoundError(entityId: Long) extends Throwable