package com.anistal.cluster.models

case class GithubModel(total_count: Int, items: Seq[GithubItem])
case class GithubItem(id: Int, name: String, description: String, html_url: String)