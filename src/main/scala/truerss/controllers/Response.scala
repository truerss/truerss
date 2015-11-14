package truerss.controllers

import truerss.util.Jsonize

sealed trait Response
case class ModelsResponse[T <: Jsonize](xs: Vector[T]) extends Response
case class OkResponse(msg: String) extends Response
case class ModelResponse[T <: Jsonize](x: T) extends Response
case class NotFoundResponse(msg: String) extends Response
case class BadRequestResponse(msg: String) extends Response
case class InternalServerErrorResponse(msg: String) extends Response