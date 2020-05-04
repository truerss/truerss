package truerss.dto

import scala.reflect.ClassTag

case class SetupKey(name: String, description: String)

case class Setup[T](key: SetupKey, value: T)
object Setup {
  def unknown[T: ClassTag](key: String): Setup[T] = {
    Setup(SetupKey(key, "Unknown key"), Default.value[T]) // ???
  }
}

case class Default[+T](default: T)

trait LowerPriorityImplicits {
  implicit def defaultNull[T <: AnyRef]: Default[T] = new Default[T](null.asInstanceOf[T])
}

object Default extends LowerPriorityImplicits {
  implicit object DefaultInt extends Default[Int](0)
  implicit object DefaultBoolean extends Default[Boolean](false)
  implicit object DefaultString extends Default[String]("")

  def value[A](implicit value: Default[A]): A = value.default
}

sealed trait AvailableValue
case class AvailableSelect(predefined: Iterable[Int]) extends AvailableValue
case class AvailableRadio(currentState: Boolean) extends AvailableValue

case class CurrentValue[T](value: T)
object CurrentValue {
  def unknown[T] = CurrentValue(Default.value[T])
}

case class AvailableSetup[T](key: String,
                             description: String,
                             options: AvailableValue,
                             value: CurrentValue[T]
                         )

case class NewSetup[T](key: String, value: CurrentValue[T])

