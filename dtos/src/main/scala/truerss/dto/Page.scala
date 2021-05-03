package truerss.dto

case class Page[T](total: Int, resources: Iterable[T])

object Page {
  def empty[T]: Page[T] = {
    Page(0, Iterable.empty[T])
  }
}
