package net.truerss.actors

import java.time.{Clock, LocalDateTime}

import net.truerss.Gen
import org.specs2.mutable.Specification
import truerss.services.actors.sync.SourceActor.UpdateTime
import truerss.util.FeedSourceDtoModelImplicits
import scala.concurrent.duration._
import scala.math.abs

class UpdateTimeTests extends Specification {

  import FeedSourceDtoModelImplicits._

  "update time" should {
    "calculate diff" in {
      val now = LocalDateTime.now(Clock.systemUTC())
      val source1 = Gen.genSource(Some(1L)).toView.copy(
        lastUpdate = now.minusHours(4),
        interval = 3
      )
      val diff1 = UpdateTime(source1)
      diff1.tickTime ==== (0 seconds)

      val source2 = source1.copy(
        lastUpdate = now.minusHours(1)
      )
      val diff2 = UpdateTime(source2)

      abs(diff2.tickTime.toSeconds - 2*60*60) must beBetween(0L, 3L)

      val source3 = source1.copy(
        lastUpdate = now.plusMinutes(10)
      )
      val diff3 = UpdateTime(source3)
      abs(diff3.tickTime.toSeconds- ((source3.interval*60+10)*60)) must beBetween(0L, 3L)
    }
  }

}
