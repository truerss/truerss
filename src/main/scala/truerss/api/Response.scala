package truerss.api

import truerss.dto._

sealed trait Response
case class SourcesResponse(sources: Vector[SourceViewDto]) extends Response
case class SourceResponse(dto: SourceViewDto) extends Response
case class SourceOverViewResponse(overview: SourceOverview) extends Response
case class FeedResponse(dto: FeedDto) extends Response
case class FeedContentResponse(content: FeedContent) extends Response
case class FeedsResponse(feeds: Vector[FeedDto]) extends Response
case class FeedsPageResponse(page: Page[FeedDto]) extends Response
case class AppPluginsResponse(view: PluginsViewDto) extends Response
case class ImportResponse(result: Iterable[SourceViewDto]) extends Response
case class SettingsResponse[_](result: Iterable[AvailableSetup[_]]) extends Response
case class SettingResponse[T](settings: AvailableSetup[T]) extends Response
case class OpmlResponse(content: String) extends Response
//

case object Ok extends Response
case class CssResponse(content: String) extends Response
case class JsResponse(content: String) extends Response
case class NotFoundResponse(msg: String) extends Response
case class BadRequestResponse(msg: String) extends Response
case class InternalServerErrorResponse(msg: String) extends Response