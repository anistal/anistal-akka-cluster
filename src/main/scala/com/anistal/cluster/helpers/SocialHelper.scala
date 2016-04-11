package com.anistal.cluster.helpers

import com.anistal.cluster.models.{GithubItem, GithubModel, TwitterModel}
import com.typesafe.config.Config
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Query, Twitter, TwitterFactory}

import scalaj.http.Http

/**
 * Helper used to communicate with Twitter's and Github's API.
 * @param config with the parameters needed to be set up by clients.
 */
case class SocialHelper(config: Config) {

  implicit val formats: Formats = DefaultFormats

  /**
   * Queries to Github's API the top ten Github's projects with the supplied keyword.
   * @param endpoint of the request.
   * @param params of the request.
   * @param numberOfElements to take from the result.
   * @return a List with the top ten Github's projects.
   */
  def githubQuery(endpoint: String,
                  params: Map[String, String],
                  numberOfElements: Int): GithubModel =
    read[GithubModel](performGithubQuery(endpoint, params, numberOfElements))

  /**
   * Searches in Twitter for a keyword.
   * @param items to search.
   * @return a list with the text of the tweets.
   */
  def twitterQuery(items: Seq[GithubItem]): Seq[TwitterModel] =
    performTwitterQuery(items)

  /**
   * Queries to Github's API the top ten Github's projects with the passed keyword.
   * @param endpoint of the request.
   * @param params of the request.
   * @param numberOfElements to take from the result.
   * @return a JValue with the top ten Github's projects.
   */
  protected def performGithubQuery(endpoint: String,
                                   params: Map[String, String],
                                         numberOfElements: Int): String =
    Http(endpoint).params(params).asString.body


  /**
   * Searches in Twitter for a keyword.
   * @param items to search.
   * @return a list of Status tweets.
   */
  protected def performTwitterQuery(items: Seq[GithubItem]): Seq[TwitterModel] = {
    import scala.collection.JavaConversions._
    val result = getTwitterInstance
      .search(new Query(items.map(_.name).mkString(" OR ")))
      .getTweets
      .toList
      .map(status => {
        val filter = items.filter(item => status.getText.contains(item.name))
        if(filter.nonEmpty)
          TwitterModel(status.getId, status.getText, filter.head.name)
        else TwitterModel(status.getId, status.getText, "none")
      }).filter(twitterModel => twitterModel.keyword != "none")
    result
  }

  /**
   * Gets an instance of a Twitter's client.
   * @return a Twitter's client.
   */
  protected def getTwitterInstance:  Twitter = {
    val consumerKey = config.getString("twitter.consumer-key")
    val consumerSecret = config.getString("twitter.consumer-secret")
    val accessToken = config.getString("twitter.access-token")
    val accessTokenSecret = config.getString("twitter.access-secret")

    val configurationBuilder = new ConfigurationBuilder()
      .setOAuthConsumerKey(consumerKey)
      .setOAuthConsumerSecret(consumerSecret)
      .setOAuthAccessToken(accessToken)
      .setOAuthAccessTokenSecret(accessTokenSecret)
      .build

    val twitterFactory = new TwitterFactory(configurationBuilder)
    twitterFactory.getInstance
  }
}
