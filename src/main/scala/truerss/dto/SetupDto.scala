package truerss.dto

import scala.reflect.ClassTag

case class SetupKey[T](name: String, description: String)

case class Setup[T](key: SetupKey[T], value: T)
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


case object SetupKeys {
  val fFeedParallelism = "parallelism"
  val fReadContent = "read_content"
  val unknown = "unknown"

  final val parallelism = Setup(SetupKey[Int](fFeedParallelism, "Number of simultaneous requests"), 10)
  final val readContent = Setup(SetupKey[Boolean](fReadContent, "Skip content"), true)
  def unknownKey[T]: SetupKey[T] = SetupKey[T](unknown, "Unknown key")


  final val predefinedSetups = Map(
    fFeedParallelism -> parallelism,
    fReadContent -> readContent
  )

  def getDefault[T: ClassTag](key: SetupKey[T]): Setup[T] = {
    predefinedSetups.get(key.name) match {
      case Some(Setup(_, v)) => Setup(key, v.asInstanceOf[T])
      case _ =>
        Setup.unknown[T](key.name)
    }
  }

  def getDefault(key: String) = {
    key match {
      case `fFeedParallelism` => parallelism
      case `fReadContent` => readContent
      case _ => Setup.unknown(key)
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

