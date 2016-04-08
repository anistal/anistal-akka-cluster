package com.anistal.cluster.constants

object ClusterConstants {


  val GithubMaxNumberOfQueries = 1
  val GithubNumberOfElements = 100
  val GithubEndpoint = "https://api.github.com/search/repositories"
  val GithubParams = Map(
    "q" -> "reactive",
    "sort" -> "stars",
    "order" -> "desc",
    "per_page" -> "100")
}
