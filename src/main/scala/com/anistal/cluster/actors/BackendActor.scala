package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import com.anistal.cluster.actors.BackendMessages._
import com.anistal.cluster.actors.FrontendMessages.FrontendMessageQuery
import com.anistal.cluster.constants.ClusterConstants
import com.anistal.cluster.helpers.SocialHelper
import com.anistal.cluster.models.{EventModelPublish, GithubItem, TwitterModel}
import com.redis.RedisClient
import com.typesafe.config.Config
import org.json4s.DefaultFormats
import org.json4s.native.Serialization._

import scala.collection.mutable.Queue

/**
 * BackendActor is used for these purposes:
 *  - Crawl reactive github's projects every x minutes.
 *  - When a frontend worker that crawls twitters is free send elements to process.
 *  - Save new elements in Redis (used in the API Rest).
 *  - Retrieve elements from Redis (used in the API Rest).
 * @param config with the configuration.
 */
class BackendActor(config: Config) extends Actor
  with ActorLogging
  with EventModelPublish {

  implicit val formats = DefaultFormats
  val actorId = ClusterConstants.AkkaBackendActorName

  val cluster = Cluster(context.system)
  var messages = Queue[Seq[GithubItem]]()

  lazy val socialHelper = new SocialHelper(config)
  lazy val redisClient = new RedisClient(config.getString("redis.host"), config.getInt("redis.port"))

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
      publish("Received BackendMessageStart (crawl Github's projects).")
      if(messages.isEmpty) {
        (1 to ClusterConstants.GithubMaxNumberOfQueries).foreach(x => {
          val githubProjects = socialHelper.githubQuery(
            ClusterConstants.GithubEndpoint,
            ClusterConstants.GithubParams + ("page" -> x.toString),
            ClusterConstants.GithubNumberOfElements)

          publish(s"Received BackendMessageStart. Processed: ${githubProjects.items.size} items")
          (messages ++= githubProjects.items.grouped(10))
        })
      } else {
        publish("Received BackendMessageStart. The queue is not empty: nothing to do.")
      }

    case BackendMessageGet =>
      if(messages.nonEmpty) {
        log.info(s"Message requested from ${sender()}")
        sender() ! FrontendMessageQuery(messages.dequeue())
      } else {
        self ! BackendMessageStart
      }

    case BackendMessagePut(twitterModel) =>
      val twitterModelJson = write[TwitterModel](twitterModel)
      if(redisClient.exists(s"${twitterModel.id}:${twitterModel.keyword}") == false) {
        redisClient.lpush("main:timeline", twitterModelJson)
        redisClient.lpush(s"${twitterModel.keyword}:timeline", twitterModelJson)
        context.system.eventStream.publish(twitterModelJson)
      }

    case BackendMessageLast10(term) =>
      val result = redisClient
        .lrange(s"$term:timeline", 0, 10)
        .map(redisOption => redisOption.flatMap(redisElement => redisElement))
        .getOrElse(Seq())
        .map(twitterModelJson =>
          read[TwitterModel](twitterModelJson))
      sender ! BackendResponse(result)
  }
}

case object BackendMessages {

  case object BackendMessageStart
  case object BackendMessageGet
  case class BackendMessageLast10(term: String)
  case class BackendResponse(twitterModels: Seq[TwitterModel])
  case class BackendMessagePut(twitterModel: TwitterModel)
}