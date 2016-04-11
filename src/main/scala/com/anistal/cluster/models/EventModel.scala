package com.anistal.cluster.models

import akka.actor.ActorContext

case class EventModel(actorId: String, message: String)

trait EventModelPublish {

  val context: ActorContext
  val actorId: String

  def publish(message: String): Unit =
    context.system.eventStream.publish(EventModel(actorId, message))
}