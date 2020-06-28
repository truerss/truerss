package net.truerss

import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.{AsResult, Result}
import org.specs2.matcher.FutureMatchers

import scala.concurrent.Future
import scala.concurrent.duration._

object FutureTestExt extends FutureMatchers {

  implicit class FutureTestExt[T](f: => Future[T])(implicit ee: ExecutionEnv, asResult: AsResult[T]) {
    def await1: Result = {
      f.await(3, 3 seconds)
    }

    def ~>(f1: T => Result): Result = {
      f.map(f1(_))(ee.executionContext).await1
    }

    def z123(f1: T => Result): Result = {
      f.map(f1(_))(ee.executionContext).await1
    }
  }

  implicit class FutureTestExt1[T](f: Future[T])(implicit ee: ExecutionEnv) {
    def ~>(f1: T => Result): Result = {
      f.map(f1(_))(ee.executionContext).await1
    }

    def z123(f1: T => Result): Result = {
      f.map(f1(_))(ee.executionContext).await1
    }
  }




}
