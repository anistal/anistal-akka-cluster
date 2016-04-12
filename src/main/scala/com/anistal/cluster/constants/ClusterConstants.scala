package com.anistal.cluster.constants

/**
 * Constants used in the application.
 */
object ClusterConstants {

  val GithubMaxNumberOfQueries = 1
  val GithubNumberOfElements = 100
  val GithubEndpoint = "https://api.github.com/search/repositories"
  val GithubParams = Map(
    "q" -> "reactive",
    "sort" -> "stars",
    "order" -> "desc",
    "per_page" -> "100")

  val AkkaClusterName = "application"
  val AkkaClusterPort = "2551"
  val AkkaBackendActorName = "backend"
  val AkkaListenerActorName = "listener"
}
