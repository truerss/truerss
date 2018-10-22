package truerss.services.actors.sync

import scala.collection.mutable.{ArrayBuffer, Map => M}

class Ticker[T](val maxUpdateCount: Int) {

  private val queue = ArrayBuffer[T]()

  private val kvMap = M[Long, T]()

  private var inProgress = 0

  def nonEmpty : Boolean = queue.nonEmpty

  def queueLength: Int = {
    queue.size
  }

  def currentProgress: Int = {
    inProgress
  }

  def getOne(key: Long): Option[T] = {
    kvMap.get(key)
  }

  def push(value: T): Option[T] = {
    if (inProgress >= maxUpdateCount) {
      queue += value
      None
    } else {
      inProgress += 1
      Some(value)
    }
  }

  def pop(): Option[Vector[T]] = {
    if (inProgress >= maxUpdateCount) {
      None
    } else {
      val tmp = queue.slice(0, maxUpdateCount)
      tmp.foreach { value =>
        queue -= value
        inProgress += 1
      }
      Some(tmp.toVector)
    }
  }

  def down(): Unit = {
    inProgress = inProgress - 1
  }

  def addOne(sourceId: Long, value: T): Unit = {
    kvMap += sourceId -> value
  }

  def deleteOne(sourceId: Long): Option[T] = {
    val ref = kvMap.get(sourceId)
    kvMap.get(sourceId).foreach{ ref =>
      queue.filter(_ == ref).foreach(queue -= _)
    }
    kvMap -= sourceId
    ref
  }

}
