package com.anistal.cluster.runners

import akka.actor._
import com.anistal.cluster.constants.ClusterConstants
import com.anistal.cluster.helpers.{ConfigHelper, RunnerHelper}

/**
 * Entry point of the application.
 */
object ClusterRunner extends App {

  implicit val config: ConfigHelper = ConfigHelper.parse(args)
  implicit val system: ActorSystem  = ActorSystem(ClusterConstants.AkkaClusterName, config.config)

  RunnerHelper.initAkkaBackend
  RunnerHelper.initAkkaFrontend
}