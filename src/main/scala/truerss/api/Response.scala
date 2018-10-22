package truerss.api

import truerss.dto.{FeedDto, PluginsViewDto, SourceViewDto}

sealed trait Response
case class SourcesResponse(xs: Vector[SourceViewDto]) extends Response
case class SourceResponse(x: Option[SourceViewDto]) extends Response
case class FeedResponse(x: FeedDto) extends Response
case class FeedsResponse(xs: Vector[FeedDto]) extends Response
case class FeedsPageResponse(xs: Vector[FeedDto], total: Int) extends Response
case class AppPluginsResponse(view: PluginsViewDto) extends Response

//

case class Ok(msg: String) extends Response
case class ModelResponse[T](x: T) // todo remove
case class CssResponse(content: String) extends Response
case class JsResponse(content: String) extends Response
case class NotFoundResponse(msg: String) extends Response
case class BadRequestResponse(msg: String) extends Response
case class InternalServerErrorResponse(msg: String) extends Response