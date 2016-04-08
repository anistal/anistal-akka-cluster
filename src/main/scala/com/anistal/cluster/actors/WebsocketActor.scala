package com.anistal.cluster.actors

import akka.actor.{ActorLogging, Actor, Props}

class WebsocketActor extends Actor with ActorLogging {

  override def preStart = {
    //context.system.eventStream.subscribe(self, classOf[String])
  }

  override def receive = {
    case value: String =>
      log.info(value)
      context.system.eventStream.publish(value)
  }
}