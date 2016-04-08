package com.anistal.cluster.models

/**
 * Twitter model that encapsulates a tweet.
 * @param text of the tweet.
 */
case class TwitterModel(id: Long, text: String, keyword: String)