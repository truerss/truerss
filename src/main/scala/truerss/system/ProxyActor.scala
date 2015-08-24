package truerss.system

import akka.actor.{ActorLogging, ActorRef, Actor, Props}
import akka.util.Timeout
import akka.pattern._
import akka.event.LoggingReceive
import truerss.controllers.BadRequestResponse
import truerss.system.network.ExtractContent

import scala.language.postfixOps
import scala.concurrent.duration._

import truerss.util.{Jsonize, SourceValidator}
import scala.concurrent.Future
import scalaz._
import Scalaz._
/**
  * Created by mike on 2.8.15.
 */
class ProxyActor(dbRef: ActorRef, networkRef: ActorRef, sourcesRef: ActorRef)
  extends Actor with ActorLogging {
  //TODO make a router

  import truerss.controllers.{ModelsResponse, ModelResponse, NotFoundResponse}
  import db._
  import network._
  import util._
  import truerss.util.Util._
  import truerss.models.{Source, Feed}
  import context.dispatcher

  implicit val timeout = Timeout(7 seconds)

  val stream = context.system.eventStream

  stream.subscribe(dbRef, classOf[SourceLastUpdate])
  stream.subscribe(dbRef, classOf[FeedContentUpdate])

  def sourceNotFound(num: Long) = NotFoundResponse(s"Source with id = ${num} not found")
  def sourceNotFound = NotFoundResponse(s"Source not found")
  def feedNotFound = NotFoundResponse(s"Feed not found")
  def feedNotFound(num: Long) = NotFoundResponse(s"Feed with id = ${num} not found")
  def optionFeedResponse[T <: Jsonize](x: Option[T]) = x match {
    case Some(m) => ModelResponse(m)
    case None => feedNotFound
  }
  def optionSourceResponse[T <: Jsonize](x: Option[T]) = x match {
    case Some(m) => ModelResponse(m)
    case None => sourceNotFound
  }
  
  def dbReceive: Receive = LoggingReceive {
    case GetAll => (dbRef ? GetAll).mapTo[Vector[Source]].map(ModelsResponse(_)) pipeTo sender

    case msg: GetSource => (dbRef ? msg).mapTo[Option[Source]].map{
      case Some(x) => ModelResponse(x)
      case None => sourceNotFound(msg.num)
    } pipeTo sender

    case msg: DeleteSource => (dbRef ? msg).mapTo[Option[Source]].map {
      case Some(source) => ModelResponse(source)
      case None => sourceNotFound(msg.num)
    } pipeTo sender

    case msg: AddSource =>
      (SourceValidator.validate(msg.source) match {
        case Right(source) =>
          (for {
            urlIsUniq <- (dbRef ? UrlIsUniq(msg.source.url)).mapTo[Int]
            nameIsUniq <- (dbRef ? NameIsUniq(msg.source.name)).mapTo[Int]
          } yield {
              if (urlIsUniq == 0 && nameIsUniq == 0) {
                (dbRef ? msg).mapTo[Long]
                  .map{x => ModelResponse(msg.source.copy(id = Some(x)))}
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
            }).flatMap(identity(_))

        case Left(errs) => Future.successful(
          BadRequestResponse(errs.toList.mkString(", ")))
      }) pipeTo sender

    case msg: UpdateSource =>
      (SourceValidator.validate(msg.source) match {
        case Right(source) =>
          (for {
            urlIsUniq <- (dbRef ? UrlIsUniq(msg.source.url, msg.num.some)).mapTo[Int]
            nameIsUniq <- (dbRef ? NameIsUniq(msg.source.name, msg.num.some)).mapTo[Int]
          } yield {
              if (urlIsUniq == 0 && nameIsUniq == 0) {
                (dbRef ? msg).mapTo[Int]
                  .map{x => ModelResponse(msg.source)}
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
            }).flatMap(identity(_))
        case Left(errs) => Future.successful(
          BadRequestResponse(errs.toList.mkString(", ")))
      }) pipeTo sender

    case msg: MarkAll => (dbRef ? msg).mapTo[Option[Source]]
      .map(optionSourceResponse) pipeTo sender

    case msg: Latest => (dbRef ? msg).mapTo[Vector[Feed]]
      .map(ModelsResponse(_)) pipeTo sender

    case msg: ExtractFeedsForSource => (dbRef ? msg).mapTo[Vector[Feed]]
      .map(ModelsResponse(_)) pipeTo sender

    case Favorites => (dbRef ? Favorites).mapTo[Vector[Feed]]
      .map(ModelsResponse(_)) pipeTo sender

    // also necessary extract content if need
    case msg: GetFeed => (dbRef ? msg).mapTo[Option[Feed]].map{
      case Some(x) => x.content match {
        case Some(content) => ModelResponse(x)
        case None => //TODO move to SourceActor
          (networkRef ? ExtractContent(msg.num, x.id.get, x.url))
            .mapTo[NetworkResult].map {
            case ExtractedEntries(sourceId, xs) =>
              dbRef ! AddFeeds(sourceId, xs.map(_.toFeed(sourceId)))
            case ExtractContentForEntry(sourceId, feedId, content) =>
              content match {
                case Some(content) =>
                  stream.publish(FeedContentUpdate(feedId, content))
                  ModelResponse(x.copy(content = content.some))
                case None => ModelResponse(x)
              }
            case ExtractError(error) => log.error(s"error on extract: $error")
            case SourceNotFound(sourceId) => log.error(s"source ${sourceId} not found")

          }
      }

        ModelResponse(x)
      case None => feedNotFound(msg.num)
    } pipeTo sender

    case msg: MarkFeed => (dbRef ? msg).mapTo[Option[Feed]]
      .map(optionFeedResponse) pipeTo sender

    case msg: UnmarkFeed => (dbRef ? msg).mapTo[Option[Feed]]
      .map(optionFeedResponse) pipeTo sender

    case msg: MarkAsReadFeed => (dbRef ? msg).mapTo[Option[Feed]]
      .map(optionFeedResponse) pipeTo sender

    case msg: MarkAsUnreadFeed => (dbRef ? msg).mapTo[Option[Feed]]
      .map(optionFeedResponse) pipeTo sender
  }

  def networkReceive: Receive = {
    case msg: NetworkInitialize => networkRef forward msg
    case msg: Grep =>
      stream.publish(SourceLastUpdate(msg.sourceId))
      (networkRef ? msg).mapTo[NetworkResult].map {
        case ExtractedEntries(sourceId, xs) =>
          dbRef ! AddFeeds(sourceId, xs.map(_.toFeed(sourceId)))
        case ExtractContentForEntry(sourceId, feedId, content) =>
        case ExtractError(error) => log.info(s"error on extract: $error")
        case SourceNotFound(sourceId) => log.info(s"source ${sourceId} not found")
      }

  }

  def utilReceive: Receive = LoggingReceive {
    case Update => sourcesRef forward Update
    case msg: UpdateOne => sourcesRef forward msg
  }

  def receive = dbReceive orElse networkReceive orElse utilReceive


}
