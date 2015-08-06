package truerss.controllers

import truerss.util.Jsonize
/**
 * Created by mike on 3.8.15.
 */
sealed trait Response
case class ModelsResponse[T <: Jsonize](xs: Vector[T]) extends Response
case class ModelResponse[T <: Jsonize](x: T) extends Response
case class NotFoundResponse(msg: String) extends Response