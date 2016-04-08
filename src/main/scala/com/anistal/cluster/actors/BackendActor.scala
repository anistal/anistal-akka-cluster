package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import com.anistal.cluster.actors.BackendMessages.{BackendMessageGet, BackendMessagePut, BackendMessageStart}
import com.anistal.cluster.actors.FrontendMessages.FrontendMessageQuery
import com.anistal.cluster.constants.ClusterConstants
import com.anistal.cluster.helpers.SocialHelper
import com.anistal.cluster.models.{GithubItem, TwitterModel}
import com.redis.RedisClient
import com.typesafe.config.Config
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

import scala.collection.mutable.Queue

class BackendActor(config: Config) extends Actor with ActorLogging {

  implicit val formats = DefaultFormats

  val cluster = Cluster(context.system)
  var messages = Queue[Seq[GithubItem]]()

  lazy val socialHelper = new SocialHelper(config)
  lazy val redisClient = new RedisClient("172.19.1.170", 6379)

  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Put(self)

  override def preStart(): Unit =
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case BackendMessageStart =>
      if(messages.isEmpty)
        (1 to ClusterConstants.GithubMaxNumberOfQueries).foreach(x => {
          log.info("Received BackendMessageStart")
          val githubProjects = socialHelper.githubQuery(
            ClusterConstants.GithubEndpoint,
            ClusterConstants.GithubParams + ("page" -> x.toString),
            ClusterConstants.GithubNumberOfElements)
          (messages ++= githubProjects.items.grouped(10))
        })
      else log.info("The queue is not empty yet")


    case BackendMessageGet =>
      if(messages.nonEmpty) {
        log.info(s"Message requested from ${sender()}")
        sender() ! FrontendMessageQuery(messages.dequeue())
      }

    case BackendMessagePut(twitterModel) =>
      val twitterModelJson = write[TwitterModel](twitterModel)

      if(redisClient.exists(s"${twitterModel.id}:${twitterModel.keyword}") == false) {
        redisClient.lpush("main:timeline", twitterModelJson)
        redisClient.lpush(s"${twitterModel.keyword}:timeline", twitterModelJson)
        redisClient.publish("main:timeline", twitterModelJson)
        redisClient.publish(s"${twitterModel.keyword}:timeline", twitterModelJson)
      }
  }
}

case object BackendMessages {

  case object BackendMessageStart
  case object BackendMessageGet
  case class BackendMessagePut(twitterModel: TwitterModel)
}