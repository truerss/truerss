package truerss.dto

import java.util.Date

import truerss.models.SourceState

sealed trait SourceDto {
  def url: String
  def name: String
  def interval: Int

  def getId: Option[Long] = None
}

case class NewSourceDto(url: String,
                        name: String,
                        interval: Int) extends SourceDto

case class UpdateSourceDto(id: Long,
                           url: String,
                           name: String,
                           interval: Int) extends SourceDto {
  override def getId: Option[Long] = Some(id)
}

case class SourceViewDto(id: Long,
                     url: String,
                     name: String,
                     interval: Int,
                     state: SourceState,
                     normalized: String,
                     lastUpdate: Date,
                     count: Int = 0) {
  def recount(x: Int): SourceViewDto = copy(count = x)
}

case class PluginDto(author: String,
                     about: String,
                     version: String,
                     pluginName: String
                    )

// TODO rename on ui
case class PluginsViewDto(
                           feed: Vector[PluginDto] = Vector.empty,
                           content: Vector[PluginDto] = Vector.empty,
                           publish: Vector[PluginDto] = Vector.empty,
                           site: Vector[PluginDto] = Vector.empty
                         )
