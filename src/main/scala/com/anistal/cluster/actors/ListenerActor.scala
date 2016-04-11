package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import com.anistal.cluster.constants.ClusterConstants
import com.anistal.cluster.models.EventModelPublish

/**
 * ListenerActor is used for retrieve new changes from the cluster.
 */
class ListenerActor extends Actor
  with ActorLogging
  with EventModelPublish {

  val cluster = Cluster(context.system)
  val actorId = ClusterConstants.AkkaBackendActorName

  override def preStart(): Unit =
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      publish(s"Member is [Up]: ${member.address}")
    case UnreachableMember(member) =>
      publish(s"Member is [Unreachable]: ${member.address}")
    case MemberRemoved(member, previousStatus) =>
      publish(s"Member is [Removed]: ${member.address}")
    case MemberExited(member) =>
      publish(s"Member is [Exited]: ${member.address}")
    case _: MemberEvent =>
      log.debug("Nothing to do")
  }
}