package com.anistal.cluster.actors

import akka.actor.Props
import akka.stream.actor.ActorPublisher

class PublishActor extends ActorPublisher[String] {

  override def preStart = {
    context.system.eventStream.subscribe(self, classOf[String])
  }

  override def receive = {
    case tweet: String =>
      if(isActive && totalDemand > 0) onNext(tweet)
  }
}

object PublishActor {
  def props: Props = Props(new PublishActor())
}