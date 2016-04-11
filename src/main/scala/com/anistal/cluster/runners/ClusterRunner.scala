package com.anistal.cluster.runners

import akka.actor._
import com.anistal.cluster.helpers.{ConfigHelper, RunnerHelper}

/**
 * Entry point of the application.
 */
object ClusterRunner extends App {

  implicit val config: ConfigHelper = ConfigHelper.parse(args)
  implicit val system: ActorSystem  = ActorSystem(config.clusterName, config.config)

  RunnerHelper.initAkkaBackend
  RunnerHelper.initAkkaFrontend
}