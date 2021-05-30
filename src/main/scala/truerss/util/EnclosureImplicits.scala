package truerss.util

import com.github.truerss.base.{Enclosure, EnclosureType}
import truerss.dto.EnclosureDto

object EnclosureImplicits {
  implicit class EnclosureDtoExt(val eDto: EnclosureDto) extends AnyVal {
    def toEnclosure: Option[Enclosure] =
      EnclosureType.withNameOption(eDto.`type`)
        .map { t =>
          Enclosure(
            `type` = t,
            url = eDto.url,
            length = eDto.length
          )
        }
  }

  implicit class EnclosureDtoStringExt(val x: String) extends AnyVal {
    def toEnclosureDto: EnclosureDto = {
      val parts = x.split('|')
      EnclosureDto(
        `type` = parts(0),
        url = parts(1),
        length = parts(2).toInt
      )
    }
  }
}
