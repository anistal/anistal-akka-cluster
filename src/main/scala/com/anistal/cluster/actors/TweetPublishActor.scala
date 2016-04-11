package com.anistal.cluster.actors

import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorPublisher

/**
 * TweetPublishActor is used to publish new tweets from the crawlers using a websocket.
 */
class TweetPublishActor extends ActorPublisher[String] with ActorLogging {

  override def preStart = {
    context.system.eventStream.subscribe(self, classOf[String])
  }

  override def receive = {
    case tweet: String =>
      if(isActive && totalDemand > 0) onNext(tweet)
  }
}

object TweetPublishActor {
  def props: Props = Props(new TweetPublishActor())
}