package net.truerss.services

import com.github.truerss.base.Errors
import org.specs2.mutable.Specification
import truerss.services.{ContentReadError, ReaderClient}
import net.truerss.ZIOMaterializer
import truerss.util.syntax

class ReaderClientTests extends Specification {

  import syntax.ext._
  import ZIOMaterializer._

  "reader client" should {
    "#fromEither" in {
      ReaderClient.
        fromEither(Some("test").right).m must beSome("test")

      ReaderClient.
        fromEither(Errors.ParsingError("boom").left).e must beLeft(ContentReadError("boom"))
    }
  }

}
