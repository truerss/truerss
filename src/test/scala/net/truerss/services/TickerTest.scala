package net.truerss.services

import org.specs2.mutable.Specification
import truerss.services.actors.sync.Ticker

class TickerTest extends Specification {

  private val z = new Ticker[Int](3)

  "Ticket" should {
    "flow" in {
      z.currentProgress ==== 0
      z.queueLength ==== 0

      z.addOne(1, 1)
      z.addOne(2, 2)
      z.addOne(3, 3)
      z.addOne(4, 4)
      z.addOne(5, 5)

      z.queueLength ==== 0
      z.currentProgress ==== 0

      z.push(1)
      z.push(2)
      z.push(3)

      z.currentProgress ==== 3
      // because < maxUpdateCount
      z.queueLength ==== 0

      z.push(4) // => in queue

      z.queueLength ==== 1
      z.currentProgress ==== 3 // nothing for progress

      z.push( 5) // waiting
      z.queueLength ==== 2
      z.currentProgress ==== 3 // nothing for progress

      z.down()
      z.currentProgress ==== 2

      val xs = z.pop()
      xs must beSome
      xs.get must contain(allOf(4, 5))
      z.currentProgress ==== 4

      z.down()
      z.down()
      z.down()
      z.down()

      z.currentProgress ==== 0
      z.queueLength ==== 0

      z.push(1) must beSome
      z.push(2) must beSome
      z.push(3) must beSome
      z.push(4) must beNone

      z.currentProgress ==== 3
      z.queueLength ==== 1

      z.pop() must beNone


      success
    }
  }

}
