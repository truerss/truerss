package truerss.api

import truerss.dto.{PluginsViewDto, SourceViewDto}
import truerss.util.Jsonize

sealed trait Response
case class SourcesResponse(xs: Vector[SourceViewDto])
case class SourceResponse(x: Option[SourceViewDto])

case class AppPluginsResponse(view: PluginsViewDto)

case class ModelsResponse[T <: Jsonize](xs: Vector[T], count: Int = -1) extends Response
case class Ok(msg: String) extends Response
case class ModelResponse[T <: Jsonize](x: T) extends Response
case class OpmlResponse(content: String) extends Response
case class CssResponse(content: String) extends Response
case class JsResponse(content: String) extends Response
case class NotFoundResponse(msg: String) extends Response
case class BadRequestResponse(msg: String) extends Response
case class InternalServerErrorResponse(msg: String) extends Response