package truerss.util

import shapeless._
import truerss.models.Source

object Lens {
  val normalized = lens[Source].normalized
  val count = lens[Source].count
}
