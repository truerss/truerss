package truerss.api

import com.github.fntz.omhs.{BodyWriter, CommonResponse}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil
import play.api.libs.json._
import truerss.dto.Processing
import truerss.services.{ContentReadError, NotFoundError, PluginNotFoundError, ValidationError}
import zio.Task

/**
  * Created by mike on 17.12.16.
  */
object HttpApi {

  val accepted = CommonResponse.empty.copy(status = HttpResponseStatus.ACCEPTED)
  val noContent = CommonResponse.empty.copy(status = HttpResponseStatus.NO_CONTENT)
  val notFound = CommonResponse.empty.copy(status = HttpResponseStatus.NOT_FOUND)
  val internal = CommonResponse.empty.copy(status = HttpResponseStatus.INTERNAL_SERVER_ERROR)

  private final val json = "application/json"
  private final val errorF = "errors"

  def flush[T](task: Task[T])(implicit writer: BodyWriter[T]): Task[CommonResponse] = {
    task.map {
      case _: Processing =>
        accepted
      case _: Unit =>
        noContent
      case x =>
        writer.write(x)
    }.fold({
      case ValidationError(errors) =>
        CommonResponse(
          status = HttpResponseStatus.BAD_REQUEST,
          contentType = json,
          content = printErrors(errors).getBytes(CharsetUtil.UTF_8)
        )
      case ContentReadError(error) =>
        CommonResponse(
          status = HttpResponseStatus.INTERNAL_SERVER_ERROR,
          contentType = json,
          content = printErrors(error :: Nil).getBytes(CharsetUtil.UTF_8)
        )
      case NotFoundError(_) => notFound
      case PluginNotFoundError => notFound
      case _ => internal
    }, identity)
  }

  def printErrors(errors: List[String]): String = {
    Json.stringify(JsObject(Seq(errorF -> JsArray(errors.map(JsString)))))
  }


}