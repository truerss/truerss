package truerss.db.driver

import play.api.libs.json._

object DbSettingJsonFormats {
  implicit lazy val settingValueFormat: Format[SettingValue] = new Format[SettingValue] {
      private val fType = "type"
      private val fValues = "values"
      private val fValue = "value"

      override def writes(o: SettingValue): JsValue = {
        o match {
          case i @ SelectableValue(xs) =>
            JsObject(
              Seq(
                fType -> JsString(i.name),
                fValues -> JsArray(xs.map(x => JsString(x)).toSeq)
              )
            )
          case i @ CheckBoxValue(currentState) =>
            JsObject(
              Seq(
                fType -> JsString(i.name),
                fValue -> JsBoolean(currentState)
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
                val values = obj.get(fValues)
                  .collect { case xs: JsArray => xs }
                  .map { arr => arr.value.collect { case JsString(value) => value } }
                  .getOrElse(Iterable.empty)
                JsSuccess(
                  SelectableValue(
                    values
                  )
                )

              case Some(JsString(CheckBoxValue.fName)) =>
                val state = obj.get(fValue).collect { case JsBoolean(x) => x }
                  .getOrElse(false)
                JsSuccess(
                  CheckBoxValue(state)
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
