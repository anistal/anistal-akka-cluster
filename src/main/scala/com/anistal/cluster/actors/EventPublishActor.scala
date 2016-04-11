package com.anistal.cluster.actors

import akka.actor.{Props, ActorLogging}
import akka.stream.actor.ActorPublisher
import com.anistal.cluster.models.EventModel

/**
 * EventPublishActor is used to publish new messages in the cluster that will be used by the websocket.
 */
class EventPublishActor extends ActorPublisher[EventModel] with ActorLogging {

  override def preStart = {
    context.system.eventStream.subscribe(self, classOf[EventModel])
  }

  override def receive = {
    case event: EventModel =>
      if(isActive && totalDemand > 0) onNext(event)
  }
}

object EventPublishActor {
  def props: Props = Props(new EventPublishActor())
}
