package com.anistal.cluster.helpers

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props, _}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.anistal.cluster.actors
import com.anistal.cluster.actors.BackendMessages.{BackendMessageLast10, BackendMessageStart, BackendResponse}
import com.anistal.cluster.actors.FrontendMessages.FrontendMessageStart
import com.anistal.cluster.actors.{BackendActor, EventPublishActor, FrontendActor, ListenerActor}
import com.anistal.cluster.models.EventModel
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

/**
 * Helper to wake up all the actors of the cluster in an orderly manner.
 */
object RunnerHelper {

  implicit val formats = DefaultFormats

  /**
   * Initializes the backend part of the cluster (listener and http server).
   * @param config with the configuration.
   * @param system with the main actor system.
   */
  def initAkkaBackend()(implicit config: ConfigHelper, system: ActorSystem): Unit = {
    if(config.isBackend == true) {
      val backend = system.actorOf(Props(new BackendActor(config.config)), "backend")
      initAkkaListener
      initAkkaHttp(backend)

      import system.dispatcher

      system.scheduler.schedule(10 seconds, 1.2 hours) {
      //system.scheduler.schedule(0 seconds, 5 seconds) {
        backend ! BackendMessageStart
      }
    }
  }

  /**
   * Initializes the frontend part of the cluster (twitter crawler).
   * @param config with the configuration.
   * @param system with the main actor system.
   */
  def initAkkaFrontend()(implicit config: ConfigHelper, system: ActorSystem): Unit = {
    if (config.isFrontend == true) {
      val frontend = system.actorOf(Props(new FrontendActor(config.config)), "frontend")

      import system.dispatcher

      import scala.concurrent.duration._
      system.scheduler.schedule(10 seconds, 1.2 minutes) {
        frontend ! FrontendMessageStart
      }
    }
  }

  // XXX Private methods

  /**
   * Initializes the httServer.
   * @param backendActor needed to retrieve data from redis and to listen events.
   * @param config with the configuration.
   * @param system with the main actor system.
   */
  private def initAkkaHttp(backendActor: ActorRef)(implicit config: ConfigHelper, system: ActorSystem): Unit = {
    if (config.isBackend == true) {
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher
      implicit val timeout: Timeout = Timeout(15.seconds)

      val tweetSource: Source[String, ActorRef] = Source.actorPublisher[String](actors.TweetPublishActor.props)
      val eventSource: Source[EventModel, ActorRef] = Source.actorPublisher[EventModel](EventPublishActor.props)

      def flowChannel(channel: String): Flow[Any, Strict, NotUsed] =
        Flow.fromSinkAndSource(Sink.ignore, tweetSource.filter(x => x.equals(channel)).map(x => {
          TextMessage.Strict(x)
        }))

      def flowEvent(): Flow[Any, Strict, NotUsed] =
        Flow.fromSinkAndSource(Sink.ignore, eventSource.map(x => {
          TextMessage.Strict(write(x))
        }))

      val routes =
        get {
          pathPrefix("css") {
            getFromResourceDirectory("web/css")
          } ~
            pathPrefix("js") {
              getFromResourceDirectory("web/js")
            } ~
            pathSingleSlash {
              getFromResource("web/index.html")
            } ~
            path("events") {
              handleWebSocketMessages(flowEvent)
            } ~
            path("last10" / Segment) { term =>
              complete {
                val response = backendActor ? BackendMessageLast10(term)
                Await.result(response, Timeout(5 seconds).duration) match {
                  case Failure(exception) => throw exception
                  case BackendResponse(value) => write(value)
                }
              }
            } ~
            path("subscribe" / Segment) { (channel) =>
              handleWebSocketMessages(flowChannel(channel))
            }
        }

      Http().bindAndHandle(routes, "0.0.0.0", 8080)
    }
  }

  /**
   * Initializes the cluster listener (used to show changes in the cluster).
   * @param config with the configuration.
   * @param system with the main actor system.
   */
  def initAkkaListener()(implicit config: ConfigHelper, system: ActorSystem): Unit = {
    system.actorOf(Props[ListenerActor], "listener")
  }
}
