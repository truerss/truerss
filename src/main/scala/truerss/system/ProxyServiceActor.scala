package truerss.system

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import truerss.controllers.BadRequestResponse
import truerss.util.{ApplicationPlugins, Jsonize, SourceValidator}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ProxyServiceActor(appPlugins: ApplicationPlugins,
                        dbRef: ActorRef,
                        sourcesRef: ActorRef, parent: ActorRef)
  extends Actor with ActorLogging {

  import context.dispatcher
  import db._
  import global._
  import network._
  import plugins.GetPluginList
  import truerss.controllers.{InternalServerErrorResponse, ModelResponse, ModelsResponse, NotFoundResponse, OkResponse}
  import truerss.models.{Enable, Feed, Neutral, Source}
  import util._
  import ws._

  implicit val timeout = Timeout(7 seconds)

  val stream = context.system.eventStream

  val publishActor = context.actorOf(Props(
    classOf[PublishPluginActor], appPlugins.publishPlugin),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishEvent])
  stream.subscribe(dbRef, classOf[SourceLastUpdate])
  stream.subscribe(dbRef, classOf[FeedContentUpdate])
  stream.subscribe(dbRef, classOf[AddFeeds])
  stream.subscribe(dbRef, classOf[SetState])

  val ok = OkResponse("ok")
  def sourceNotFound(x: Numerable) =
    NotFoundResponse(s"Source with id = ${x.num} not found")
  def sourceNotFound = NotFoundResponse(s"Source not found")
  def feedNotFound = NotFoundResponse(s"Feed not found")
  def feedNotFound(num: Long) = NotFoundResponse(s"Feed with id = ${num} not found")
  def optionFeedResponse[T <: Jsonize](x: Option[T]) = x match {
    case Some(m) => ModelResponse(m)
    case None => feedNotFound
  }

  def getState(url: String) = if (appPlugins.matchUrl(new java.net.URL(url))) {
    Enable
  } else {
    Neutral
  }

  def addOrUpdate[T <: Jsonize](msg: Sourcing,
                                f: Long => ModelResponse[T]) = {
    SourceValidator.validate(msg.source) match {
      case Right(source) =>
        val state = getState(msg.source.url)
        val newSource = msg.source.copy(state = state)
        val (newMsg, checkUrl, checkName) = msg match {
          case AddSource(_) => (AddSource(newSource), UrlIsUniq(msg.source.url),
                NameIsUniq(msg.source.name))
          case UpdateSource(sId, _) =>
            (UpdateSource(sId, newSource), UrlIsUniq(msg.source.url, msg.source.id),
              NameIsUniq(msg.source.name, msg.source.id))
        }

        (for {
          urlIsUniq <- (dbRef ? checkUrl).mapTo[Int]
          nameIsUniq <- (dbRef ? checkName).mapTo[Int]
        } yield {
            val tofb = (x: String) => Future.successful(BadRequestResponse(x))
            val u = s"Url '${newSource.url}' already present in db"
            val n = s"Name '${newSource.name}' not unique"
            (urlIsUniq, nameIsUniq) match {
              case (0, 0) =>
                (dbRef ? newMsg).mapTo[Long].map(f)
              case (0, _) => tofb(n)
              case (_, 0) => tofb(u)
              case (_, _) => tofb(s"$u, $n")
            }
          }).flatMap(identity)

      case Left(errs) => Future.successful(
        BadRequestResponse(errs.toList.mkString(", ")))
    }
  }

  def dbReceive: Receive = {
    case OnlySources => dbRef forward OnlySources

    case GetAll =>
      (for {
        counts <- (dbRef ? FeedCount(false)).mapTo[Vector[(Long, Int)]]
        sources <- (dbRef ? GetAll).mapTo[Vector[Source]]
      } yield {
        val map = counts.toMap
        ModelsResponse(
          sources.map(s => s.recount(map.getOrElse(s.id.get, 0)))
        )
      }) pipeTo sender

    case msg: Unread => (dbRef ? msg).mapTo[Vector[Feed]]
      .map(ModelsResponse(_)) pipeTo sender

    case msg: DeleteSource =>
      (dbRef ? msg).mapTo[Option[Source]].map {
        case Some(source) =>
          sourcesRef ! SourceDeleted(source)
          stream.publish(SourceDeleted(source)) // => ws
          ok
        case None => sourceNotFound(msg)
      } pipeTo sender

    case msg : Numerable => (dbRef ? msg).mapTo[Option[Source]].map{
      case Some(x) => ModelResponse(x)
      case None => sourceNotFound(msg)
    } pipeTo sender

    case msg: AddSource =>
      addOrUpdate(
        msg,
        (x: Long) => {
          val source = msg.source.copy(id = Some(x))
          val newSource = source.recount(0).withState(getState(source.url))
          stream.publish(SourceAdded(newSource))
          sourcesRef ! NewSource(newSource)
          ModelResponse(newSource)
        }
      ) pipeTo sender

    case msg: UpdateSource =>
      addOrUpdate(
        msg,
        (x: Long) => {
          val source = msg.source.copy(id = Some(x))
          val frontendSource = source.recount(0)
          stream.publish(SourceUpdated(frontendSource))
          //TODO update source actor
          ModelResponse(frontendSource)
        }
      ) pipeTo sender

    case msg: ExtractFeedsForSource =>
      (for {
        feeds <- (dbRef ? msg).mapTo[Vector[Feed]]
        count <- (dbRef ? FeedCountForSource(msg.sourceId)).mapTo[Int]
      } yield ModelsResponse(feeds, count)
      ) pipeTo sender

    case msg @ (_: Latest | _ : Favorites.type) =>
      (dbRef ? msg).mapTo[Vector[Feed]]
        .map(ModelsResponse(_)) pipeTo sender

    case MarkAll => (dbRef ? MarkAll).mapTo[Long]
      .map(l => OkResponse(s"$l")) pipeTo sender

    // also necessary extract content if need
    case msg: GetFeed =>
      (dbRef ? msg).mapTo[Option[Feed]].flatMap {
        case Some(x) => x.content match {
          case Some(content) =>
            log.info("feed have content")
            Future.successful(ModelResponse(x))
          case None =>
            (sourcesRef ? ExtractContent(x.sourceId, x.id.get, x.url))
              .mapTo[NetworkResult].map {
              case ExtractedEntries(sourceId, xs) =>
                InternalServerErrorResponse("Unexpected message")
              case ExtractContentForEntry(sourceId, feedId, content) =>
                content match {
                  case Some(content) =>
                    stream.publish(FeedContentUpdate(feedId, content))
                    ModelResponse(x.copy(content = Some(content)))
                  case None => ModelResponse(x)
                }
              case ExtractError(error) =>
                log.error(s"error on extract from ${x.sourceId} -> ${x.url}: $error")
                InternalServerErrorResponse(error)

              case SourceNotFound(sourceId) =>
                log.error(s"source ${sourceId} not found")
                InternalServerErrorResponse(s"source ${sourceId} not found")
            }
        }

        case None => Future.successful(feedNotFound(msg.num))
    } pipeTo sender

    case msg : MarkFeed =>
      (dbRef ? msg).mapTo[Option[Feed]]
        .map{ x =>
        x.foreach(f => stream.publish(PublishEvent(f)))
        optionFeedResponse(x)
      } pipeTo sender

    case msg @ (_ : UnmarkFeed |
                _ : MarkAsReadFeed | _ : MarkAsUnreadFeed)  =>
      (dbRef ? msg).mapTo[Option[Feed]]
        .map(optionFeedResponse) pipeTo sender

    case msg: SetState =>
      stream.publish(msg)

  }

  def networkReceive: Receive = {
    case msg: NewFeeds => stream.publish(msg)
  }

  def utilReceive: Receive = {
    case msg: Notify => stream.publish(msg)
    case msg @ ( _ : Update.type | _ : UpdateOne) =>
      sourcesRef forward msg
      sender ! ok
  }

  def pluginReceive: Receive = {
    case GetPluginList => sender ! ModelResponse(appPlugins)
  }

  def systemReceive: Receive = {
    case RestartSystem =>
      sourcesRef ! RestartSystem
      sender ! ok

    case StopSystem =>
      parent ! StopSystem
      sender ! ok

    case StopApp =>
      parent ! StopApp
      sender ! ok
  }

  def receive = dbReceive orElse
    networkReceive orElse
    utilReceive orElse
    pluginReceive orElse
    systemReceive

  override def unhandled(m: Any) = log.warning(s"Unhandled $m")

}
