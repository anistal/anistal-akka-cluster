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

class FrontendActor(config: Config) extends Actor with ActorLogging {

  implicit val formats = DefaultFormats

  val cluster = Cluster(context.system)
  val mediator = DistributedPubSub(context.system).mediator

  lazy val socialHelper = new SocialHelper(config)
  var frontendCache = scala.collection.mutable.Map[Long, Long]()

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

      val query = items.mkString(" OR ")
      socialHelper.twitterQuery(query).foreach(tweet => {
        if(frontendCache.contains(tweet.id) == false) {
          mediator ! Send(path = "/user/backend", msg = BackendMessagePut(tweet), localAffinity = true)
          frontendCache.put(tweet.id, tweet.id)
        }
      })
  }
}

case object FrontendMessages {

  case object FrontendMessageStart
  case class FrontendMessageQuery(items: Seq[GithubItem])
}