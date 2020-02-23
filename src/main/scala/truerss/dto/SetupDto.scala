package truerss.dto

import scala.reflect.ClassTag

case class SetupKey[T](name: String, description: String)

case class Setup[T](key: SetupKey[T], value: T)
object Setup {
  def unknown[T](key: String): Setup[T] = {
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


case object SetupKeys {
  val feedParallelism = "parallelism"
  val readContent = "read_content"
  val unknown = "unknown"

  final val parallelismKey = SetupKey[Int](feedParallelism, "Number of simultaneous requests")
  final val readContentKey = SetupKey[Boolean](readContent, "Skip content")
  final val unknownKey = SetupKey(unknown, "Unknown key")


  final val predefinedSetups = Map(
    parallelismKey.name -> Setup(parallelismKey, 10),
    readContentKey.name -> Setup(readContentKey, true)
  )

  def getDefault[T: ClassTag](key: SetupKey[T]): Setup[T] = {
    predefinedSetups.get(key.name) match {
      case Some(t: T) => Setup(key, t)
      case _ =>  Setup.unknown(key.name)
    }
  }

  def getDefault[T: ClassTag](key: String): Setup[T] = {
    predefinedSetups.get(key) match {
      case Some(t: T) => Setup[T](t.key.asInstanceOf[SetupKey[T]], t)
      case _ =>  Setup.unknown(key)
    }
  }
}

sealed trait AvailableValue
case class AvailableSelect(predefined: Iterable[Int]) extends AvailableValue
case class AvailableCheckBox(currentState: Boolean) extends AvailableValue

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

