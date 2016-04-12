package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import com.anistal.cluster.constants.ClusterConstants
import com.anistal.cluster.models.EventModelPublish

/**
 * ListenerActor is used for retrieve new changes from the cluster.
 */
class EventListenerActor extends Actor
  with ActorLogging
  with EventModelPublish {

  val cluster = Cluster(context.system)
  val actorId = ClusterConstants.AkkaBackendActorName
  val messages = Seq()

  override def preStart(): Unit =
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      val message = s"Member is [Up]: ${member.address}"
      if(messages.contains(message) == false) {
        publish(message)
      }
    case UnreachableMember(member) =>
      val message = s"Member is [Unreachable]: ${member.address}"
      if(messages.contains(message) == false) {
        publish(message)
      }
    case MemberRemoved(member, previousStatus) =>
      val message = s"Member is [Removed]: ${member.address}"
      if(messages.contains(message) == false) {
        publish(message)
      }
    case MemberExited(member) =>
      val message = s"Member is [Exited]: ${member.address}"
      if(messages.contains(message) == false) {
        publish(message)
      }
    case _: MemberEvent =>
      log.debug("Nothing to do")
  }
}