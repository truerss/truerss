package truerss.db

import play.api.libs.json._

object DbSettingJsonFormats {
  implicit lazy val settingValueFormat: Format[SettingValue] = new Format[SettingValue] {
    private val fType = "type"
    private val fValues = "values"
    private val fValue = "value"
    private val fDefault = "default"

    override def writes(o: SettingValue): JsValue = {
      o match {
        case i @ SelectableValue(xs, defaultValue) =>
          JsObject(
            Seq(
              fType -> JsString(i.name),
              fValues -> JsArray(xs.map(x => JsNumber(x)).toSeq),
              fDefault -> JsNumber(i.defaultValue)
            )
          )
        case i @ RadioValue(defaultValue) =>
          JsObject(
            Seq(
              fType -> JsString(i.name),
              fDefault -> JsBoolean(defaultValue)
            )
          )
      }
    }

    override def reads(json: JsValue): JsResult[SettingValue] = {
      json match {
        case JsObject(obj) =>
          val tpe = obj.get(fType)
          tpe match {
            case Some(JsString(SelectableValue.fName)) =>
              val res = for {
                values <- obj.get(fValues)
                  .collect { case xs: JsArray => xs }
                  .map { arr => arr.value.collect { case JsNumber(value) => value.toInt } }
                default <- obj.get(fDefault).map(_.as[Int])
              } yield {
                SelectableValue(
                  values,
                  defaultValue = default
                )
              }
              res.map(JsSuccess(_)).getOrElse(JsError("Failed to read"))

            case Some(JsString(RadioValue.fName)) =>
              val state = obj.get(fValue).collect { case JsBoolean(x) => x }
                .getOrElse(false)
              JsSuccess(
                RadioValue(state)
              )

            case _ =>
              JsError(s"Unexpected type: $tpe")
          }

        case _ =>
          JsError("Object is required")
      }
    }
  }
}
