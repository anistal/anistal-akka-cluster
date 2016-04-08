package com.anistal.cluster.runners

import akka.NotUsed
import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.model.ws.TextMessage.Strict
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.anistal.cluster.actors
import com.anistal.cluster.actors.FrontendMessages.FrontendMessageStart
import com.anistal.cluster.actors._
import com.anistal.cluster.helpers.ConfigHelper

import scala.io.StdIn

object ClusterRunner extends App {

  val config:ConfigHelper = ConfigHelper.parse(args)

  implicit val system = ActorSystem(config.clusterName, config.config)
  system.actorOf(Props[ListenerActor], "listener")

  if(config.isBackend == true) {
    //val backend = system.actorOf(Props(new BackendActor(config.config)), "backend")

    //      import system.dispatcher
    //
    //      import scala.concurrent.duration._
    //      system.scheduler.schedule(10 seconds, 1.2 hours) {
    //        backend ! BackendMessageStart
    //      }

    initWebsocketServer

  } else if(config.isFrontend == true) {
    val frontend = system.actorOf(Props(new FrontendActor(config.config)), "frontend")

    import system.dispatcher

    import scala.concurrent.duration._
    system.scheduler.schedule(10 seconds, 1.2 minutes) {
      frontend ! FrontendMessageStart
    }
  }



  def initWebsocketServer(implicit system: ActorSystem): Unit = {

    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val source: Source[String, ActorRef] = Source.actorPublisher[String](actors.PublishActor.props)

    def flowChannel(channel: String): Flow[Any, Strict, NotUsed] = {
      Flow.fromSinkAndSource(Sink.ignore, source.filter(x => x.equals(channel)).map(x => {
        TextMessage.Strict(x)
      }))
    }

    val websocketActor = system.actorOf(Props[WebsocketActor], "websocketActor")

    import scala.concurrent.duration._
    system.scheduler.schedule(0 seconds, 5 seconds) {
      websocketActor ! "hola"
    }

    val routes =
      logRequestResult("akka-http-test") {
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
            path("subscribe" / Segment) { (channel) =>
              handleWebSocketMessages(flowChannel(channel))
            }
        }
      }

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ ⇒ system.terminate())
  }

}