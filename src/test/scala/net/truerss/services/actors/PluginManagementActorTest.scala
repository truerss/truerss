package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api.{AppPluginsResponse, CssResponse, JsResponse}
import truerss.dto.PluginsViewDto
import truerss.services.ApplicationPluginsService
import truerss.services.actors.PluginManagementActor

class PluginManagementActorTest extends TestKit(ActorSystem("PluginManagementActor"))
  with SpecificationLike with Mockito {


  "PluginManagementActor" should {
    "return js" in new MyTest {
      val code = "js"
      service.js.returns(code)
      pass(PluginManagementActor.GetJs) {
        case msg: JsResponse =>
          msg.content ==== code
      }
    }

    "return css" in new MyTest {
      val code = "css"
      service.css.returns(code)
      pass(PluginManagementActor.GetCss) {
        case msg: CssResponse =>
          msg.content ==== code
      }
    }

    "return view" in new MyTest {
      val v = PluginsViewDto()
      service.view.returns(v)
      pass(PluginManagementActor.GetPluginList) {
        case msg: AppPluginsResponse =>
          msg.view ==== v
      }
    }
  }

  private class MyTest extends Scope {
    val me = TestProbe()
    val service = mock[ApplicationPluginsService]

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      TestActorRef(new PluginManagementActor(service)).tell(msg, me.ref)
      me.expectMsgPF()(pf)
    }
  }


}
