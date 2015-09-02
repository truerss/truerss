package truerss.system

import akka.actor.{ActorLogging, ActorRef, Actor, Props}
import akka.util.Timeout
import akka.pattern._
import akka.event.LoggingReceive
import truerss.controllers.{InternalServerErrorResponse, BadRequestResponse}
import truerss.system.db.OnlySources
import truerss.system.network.ExtractContent

import scala.language.postfixOps
import scala.concurrent.duration._

import truerss.util.{Jsonize, SourceValidator, ApplicationPlugins}
import scala.concurrent.Future
import scalaz._
import Scalaz._

class ProxyServiceActor(appPlugins: ApplicationPlugins,
                        dbRef: ActorRef,
                        networkRef: ActorRef,
                        sourcesRef: ActorRef)
  extends Actor with ActorLogging {

  import truerss.controllers.{
    ModelsResponse, ModelResponse, NotFoundResponse, InternalServerErrorResponse}
  import db._
  import network._
  import util._
  import ws._
  import truerss.util.Util._
  import truerss.models.{Source, Feed}
  import context.dispatcher

  implicit val timeout = Timeout(7 seconds)

  val stream = context.system.eventStream

  stream.subscribe(dbRef, classOf[SourceLastUpdate])
  stream.subscribe(dbRef, classOf[FeedContentUpdate])
  stream.subscribe(dbRef, classOf[AddFeeds])

  def sourceNotFound(x: Numerable) =
    NotFoundResponse(s"Source with id = ${x.num} not found")
  def sourceNotFound = NotFoundResponse(s"Source not found")
  def feedNotFound = NotFoundResponse(s"Feed not found")
  def feedNotFound(num: Long) = NotFoundResponse(s"Feed with id = ${num} not found")
  def optionFeedResponse[T <: Jsonize](x: Option[T]) = x match {
    case Some(m) => ModelResponse(m)
    case None => feedNotFound
  }

  def addOrUpdate[T <: Jsonize](msg: Sourcing,
                                checkUrl: UrlIsUniq,
                                checkName: NameIsUniq,
                                f: Long => ModelResponse[T]) = {
    SourceValidator.validate(msg.source) match {
      case Right(source) =>
        (for {
          urlIsUniq <- (dbRef ? checkUrl).mapTo[Int]
          nameIsUniq <- (dbRef ? checkName).mapTo[Int]
        } yield {
            if (urlIsUniq == 0 && nameIsUniq == 0) {
              (dbRef ? msg).mapTo[Long].map(f)
            } else {
              val urlError = if (urlIsUniq > 0) {
                "Url already present in db"
              } else { "" }
              val nameError = if(nameIsUniq > 0) {
                "Name not unique"
              } else {
                ""
              }
              val errs = Vector(urlError, nameError).filterNot(_.isEmpty)
              Future.successful(BadRequestResponse(errs.mkString(", ")))
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
          sources.map(s => s.convert(map.get(s.id.get).getOrElse(0)))
        )
      }) pipeTo sender

    case msg : Numerable => (dbRef ? msg).mapTo[Option[Source]].map{
      case Some(x) => ModelResponse(x)
      case None => sourceNotFound(msg)
    } pipeTo sender

    case msg: AddSource =>
      val newSource = msg.source.copy(plugin = appPlugins.matchUrl(msg.source.url))
      val newMsg = msg.copy(source = newSource)
      addOrUpdate(
        newMsg,
        UrlIsUniq(msg.source.url),
        NameIsUniq(msg.source.name),
        (x: Long) => {
          val source = newMsg.source.copy(id = Some(x)).convert(0)
          stream.publish(SourceAdded(source))
          ModelResponse(source)
        }
      ) pipeTo sender

    case msg: UpdateSource =>
      val newSource = msg.source.copy(plugin = appPlugins.matchUrl(msg.source.url))
      val newMsg = msg.copy(source = newSource)
      addOrUpdate(
        newMsg,
        UrlIsUniq(msg.source.url, msg.num.some),
        NameIsUniq(msg.source.name, msg.num.some),
        (x: Long) => {
          ModelResponse(newMsg.source) }
      ) pipeTo sender

    case msg @ (_: Latest | _: ExtractFeedsForSource | _ : Favorites.type) =>
      (dbRef ? msg).mapTo[Vector[Feed]]
        .map(ModelsResponse(_)) pipeTo sender

    // also necessary extract content if need
    case msg: GetFeed =>
      (dbRef ? msg).mapTo[Option[Feed]].flatMap {
        case Some(x) => x.content match {
          case Some(content) =>
            log.info("feed have content")
            Future.successful(ModelResponse(x))
          case None => //TODO move to SourceActor ?
            (networkRef ? ExtractContent(x.sourceId, x.id.get, x.url))
              .mapTo[NetworkResult].map {
              case ExtractedEntries(sourceId, xs) =>
                InternalServerErrorResponse("Unexpected message")
              case ExtractContentForEntry(sourceId, feedId, content) =>
                content match {
                  case Some(content) =>
                    stream.publish(FeedContentUpdate(feedId, content))
                    ModelResponse(x.copy(content = content.some))
                  case None => ModelResponse(x)
                }
              case ExtractError(error) =>
                log.error(s"error on extract: $error")
                InternalServerErrorResponse(error)

              case SourceNotFound(sourceId) =>
                log.error(s"source ${sourceId} not found")
                InternalServerErrorResponse(s"source ${sourceId} not found")
            }
        }

        case None => Future.successful(feedNotFound(msg.num))
    } pipeTo sender

    case msg @ (_ : MarkFeed | _ : UnmarkFeed |
                _ : MarkAsReadFeed | _ : MarkAsUnreadFeed)  =>
      (dbRef ? msg).mapTo[Option[Feed]]
        .map(optionFeedResponse) pipeTo sender

  }

  def networkReceive: Receive = {
    case msg: Grep =>
      stream.publish(SourceLastUpdate(msg.sourceId))
      (networkRef ? msg).mapTo[NetworkResult].map {
        case ExtractedEntries(sourceId, xs) =>
          dbRef ! AddFeeds(sourceId, xs.map(_.toFeed(sourceId)))
        case ExtractContentForEntry(sourceId, feedId, content) =>
        case ExtractError(error) => log.info(s"error on extract: $error")
        case SourceNotFound(sourceId) => log.info(s"source ${sourceId} not found")
      }

    case msg: NewFeeds => stream.publish(msg)
  }

  def utilReceive: Receive = LoggingReceive {
    case msg @ ( _ : Update.type | _ : UpdateOne) => sourcesRef forward msg
  }

  def receive = dbReceive orElse networkReceive orElse utilReceive

  override def unhandled(m: Any) = log.warning(s"Undhandled $m")

}
