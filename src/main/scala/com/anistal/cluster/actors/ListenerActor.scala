package com.anistal.cluster.actors

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._

class ListenerActor extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit =
    cluster.subscribe(
      subscriber = self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info(s"Member is [Up]: ${member.address}")
    case UnreachableMember(member) =>
      log.info(s"Member is [Unreachable]: ${member.address}")
    case MemberRemoved(member, previousStatus) =>
      log.info(s"Member is [Removed]: ${member.address}")
    case MemberExited(member) =>
      log.info(s"Member is [Exited]: ${member.address}")
    case _: MemberEvent =>
      log.debug("Nothing to do")
  }

}