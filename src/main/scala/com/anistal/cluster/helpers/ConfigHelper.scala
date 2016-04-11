package com.anistal.cluster.helpers

import java.net.NetworkInterface

import com.anistal.cluster.helpers.ConfigHelper._
import com.typesafe.config.ConfigFactory._
import com.typesafe.config._
import scala.collection.JavaConversions._

/**
 * Used to parse all the configuration of the cluster
 * @param isBackend if it is a backend node.
 * @param isFrontend if it is a frontend node.
 * @param configParams with all configuration parameters
 */
case class ConfigHelper(isBackend: Boolean = false,
                        isFrontend: Boolean = false,
                        configParams: Map[String, String] = Map.empty) {

  lazy val config = getTypesafeConfig
  lazy val clusterName = config.getString(ClusterNamePath)

  private def getTypesafeConfig(): Config = {
    val config = load(
      getClass.getClassLoader,
      ConfigResolveOptions.defaults.setAllowUnresolved(true))

    val clusterName = config.getString(ClusterNamePath)
    val configPath = if(isBackend) BackendConf else FrontendConf
    val ip = getHostIP().getOrElse("127.0.0.1")
    val ipConfigValue = ConfigValueFactory.fromAnyRef(ip)
    val configMap = ConfigValueFactory.fromMap(configParams)
    val consumerKey = configMap.get("ck")
    val consumerSecret = configMap.get("cs")
    val accessToken = configMap.get("at")
    val accessSecret = configMap.get("ats")
    val clusterSeedConfig = Option(configMap.get("seed")).map(seed => {
      s"""
         |akka {
         |  cluster {
         |    seed-nodes = ["akka.tcp://$clusterName@${seed.unwrapped().toString}"]
         |  }
         |}
      """.stripMargin
    }).mkString("")
    val redisHost = Option(configMap.get("rh")).getOrElse(ConfigValueFactory.fromAnyRef("127.0.0.1"))
    val redisPort = Option(configMap.get("rp")).getOrElse(ConfigValueFactory.fromAnyRef(6379))

    (ConfigFactory.parseString(clusterSeedConfig))
      .withValue("clustering.ip", ipConfigValue)
      .withValue("consumer.key", consumerKey)
      .withValue("consumer.secret", consumerSecret)
      .withValue("access.token", accessToken)
      .withValue("access.secret", accessSecret)
      .withValue("redis.host", redisHost)
      .withValue("redis.port", redisPort)
      .withFallback(ConfigFactory.parseResources(configPath))
      .withFallback(config)
      .resolve
  }
}

object ConfigHelper {

  val BackendConf = "node.seed.conf"
  val FrontendConf = "node.cluster.conf"

  private val ClusterNamePath = "clustering.cluster.name"

  def parse(args: Seq[String]): ConfigHelper = {

    val parser = new scopt.OptionParser[ConfigHelper]("akka-cluster") {
      head("akka-cluster", "1.0")

      opt[Map[String, String]]("backend")
        .optional()
        .valueName("ck=<consumerKey>, cs=<consumerSecret>, at=<accessToken>, ats=<accessTokenSecret>")
        .action { (x, c) =>
          c.copy(configParams = x, isBackend = true)
        }.text ("backend node: ck=consumerKey, cs=consumerSecret, at=accessToken, ats=accessTokenSecret")

      opt[Map[String, String]]("frontend")
        .optional()
        .valueName("backendAddress consumerKey consumerSecret accessToken accessTokenSecret")
        .action { (x, c) =>
          c.copy(configParams = x, isFrontend = true)
        }.text("set the seed address and Twitter's credentials")

      help("help").text("prints this usage text")

      checkConfig { c =>
        if (c.isBackend == false && c.isFrontend == false) {
          failure("You must define a backend or a frontend.")
        } else if (c.isBackend == true && hasAllTwitterParams(c.configParams) == false) {
          failure(""""
                    |You must define twitter credentials:
                    |akka-cluster ck=<consumerKey>, cs=<consumerSecret>, at=<accessToken>, ats=<accessTokenSecret>
                  """.stripMargin)
        } else if (c.isFrontend == true
          && (c.configParams.contains("seed") == false || hasAllTwitterParams(c.configParams) == false)) {
          failure("You must define the seed node and twitter credentials")
        } else success
      }
    }

    parser.parse(args, ConfigHelper(false, false, Map.empty)) match {
      case None => throw new IllegalStateException("No configuration parsed")
      case Some(config) => config
    }
  }

  protected def hasAllTwitterParams(conf: Map[String, String]): Boolean =
    conf.contains("ck") && conf.contains("cs") && conf.contains("at") && conf.contains("ats")

  protected def getHostIP(): Option[String] = {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    val interface = interfaces.find(_.getName.equals("eth0"))

    interface.flatMap(inet =>
      inet.getInetAddresses
        .find(_.isSiteLocalAddress)
        .map(_.getHostAddress))
  }
}