package truerss.dto

import java.util.Date

import truerss.models.SourceState

case class NewSourceDto(url: String,
                        name: String,
                        interval: Int)

case class UpdateSourceDto(id: Long,
                           url: String,
                           name: String,
                           interval: Int)

/*
def toSource = {
    Source(
      id = id,
      url = url,
      name = name,
      interval = interval,
      state = Neutral,
      normalized = name.normalize,
      lastUpdate = new Date()
    )
  }
 */

case class SourceDto(id: Option[Long],
                     url: String,
                     name: String,
                     interval: Int,
                     state: SourceState,
                     normalized: String,
                     lastUpdate: Date,
                     count: Int = 0)
