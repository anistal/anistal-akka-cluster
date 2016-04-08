name := "anistal-akka-cluster"
version := "0.1"
scalaVersion := "2.11.5"

enablePlugins(JavaAppPackaging)

maintainer := "Alvaro Nistal"
packageSummary := s"My akka cluster experiment"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-cluster" % "2.4.3",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.3",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.3",
//  "com.typesafe.akka" %% "akka-http-core-experimental" % "2.4.3",
  "com.typesafe.akka" %% "akka-stream" % "2.4.3",
  "com.github.scopt" %% "scopt" % "3.4.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "org.json4s" %% "json4s-ext" % "3.3.0",
  "org.twitter4j" % "twitter4j-core" % "4.0.4",
  "org.scalaj" %% "scalaj-http" % "2.2.1",
  "net.debasishg" %% "redisclient" % "3.0"
)

