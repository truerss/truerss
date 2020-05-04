package truerss.util

import scala.concurrent.Future
import scala.util.Either

object syntax {

  type \/[L, R] = Either[L, R]

  final class OptionExt[T](val x: T) extends AnyVal {
    def some: Option[T] = Some(x)
  }
  trait WithOptionExt {
    implicit def WithOptionExt[T](x: T): OptionExt[T] = new OptionExt(x)
  }

  final class EitherExt[T](val x: T) extends AnyVal {
    def right[R]: Either[R, T] = Right(x)
    def left[R]: Either[T, R] = Left(x)
  }
  trait WithEitherExt {
    implicit def WithEitherExt[T](x: T): EitherExt[T] = new EitherExt(x)
  }

  object ext extends WithOptionExt with WithEitherExt

  final class FutureExt[T](val x: T) extends AnyVal {
    def toF: Future[T] = Future.successful(x)
  }

  trait WithFutureSyntax extends {
    implicit def WithFutureExt[T](x: T): FutureExt[T] = new FutureExt(x)
  }


  object future extends WithFutureSyntax

}
