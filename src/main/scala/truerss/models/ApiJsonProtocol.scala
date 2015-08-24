package truerss.models

import truerss.util.Jsonize
import spray.json._
/**
 * Created by mike on 2.8.15.
 */
object ApiJsonProtocol extends DefaultJsonProtocol {

  implicit object DateFormat extends JsonFormat[java.util.Date] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    override def read(json:JsValue): java.util.Date = format.parse(json.convertTo[String])
    override def write(date:java.util.Date) = format.format(date).toJson
  }

  implicit val sourceFormat = jsonFormat8(Source)
  implicit val feedFormat = jsonFormat12(Feed)
  implicit val frontendSourceFormat = jsonFormat3(FrontendSource)
  implicit val sourceForFrontend = jsonFormat8(SourceForFrontend)

  implicit def jsonizeWriter: JsonWriter[Jsonize] = new JsonWriter[Jsonize] {
    def write(x: Jsonize) = x match {
      case x: Source => sourceFormat.write(x)
      case x: SourceForFrontend => sourceForFrontend.write(x)
      case x: Feed => feedFormat.write(x)
    }
  }

  implicit def jsonizeVectorWriter: JsonWriter[Vector[Jsonize]] = new JsonWriter[Vector[Jsonize]] {
    override def write(xs: Vector[Jsonize]) = JsArray(xs.map(_.toJson))
  }





}
