package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Send
import com.anistal.cluster.actors.BackendMessages.{BackendMessageGet, BackendMessagePut}
import com.anistal.cluster.actors.FrontendMessages.{FrontendMessageQuery, FrontendMessageStart}
import com.anistal.cluster.helpers.SocialHelper
import com.anistal.cluster.models.GithubItem
import com.typesafe.config.Config
import org.json4s.DefaultFormats

/**
 * FrontendActor is used for these purposes:
 *   - When it is not processing any element send a message to the backend reporting that it is free.
 *   - Crawling tweets from twitter and sending new tweets to the backend.
 * @param config with the configuration.
 */
class FrontendActor(config: Config) extends Actor with ActorLogging {

  implicit val formats = DefaultFormats

  val cluster = Cluster(context.system)
  val mediator = DistributedPubSub(context.system).mediator

  lazy val socialHelper = new SocialHelper(config)

  override def preStart(): Unit =
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case FrontendMessageStart =>
      log.info("Received FrontendMessageStart")
      mediator ! Send(path = "/user/backend", msg = BackendMessageGet, localAffinity = true)

    case FrontendMessageQuery(items) =>
      log.info("Received FrontendMessageQuery")

      socialHelper.twitterQuery(items).foreach(tweet => {
        mediator ! Send(path = "/user/backend", msg = BackendMessagePut(tweet), localAffinity = true)
      })
  }
}

case object FrontendMessages {

  case object FrontendMessageStart
  case class FrontendMessageQuery(items: Seq[GithubItem])
}