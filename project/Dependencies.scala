import sbt._

object Versions {
  val scala213 = "2.13.4"
  val scala212 = "2.12.11"
  val scalaTest = "3.1.1"
  val akka = "2.6.10"
  val akkaHttp = "10.2.3"
  val akkaStreamKafka = "2.0.5"
  val logback = "1.2.3"
  val slf4j = "1.7.30"
  val testContainer = "0.39.0"
}

object Dependencies {
  val scala213Versions = "2.13.4"
  val scala212Versions = "2.12.11"

  val scalatest = "org.scalatest" %% "scalatest" % Versions.scalaTest

  object akka {
    val actor = "com.typesafe.akka" %% "akka-actor" % Versions.akka
    val actorTyped = "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % Versions.akka
    val serializeJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % Versions.akka
    val stream = "com.typesafe.akka" %% "akka-stream" % Versions.akka
    val streamKafka = "com.typesafe.akka" %% "akka-stream-kafka" % Versions.akkaStreamKafka

    val clusterShardingTyped = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % Versions.akka
    val persistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % Versions.akka
    val http = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
    val httpSprayJson = "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp

    val testKit = "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test
    val testKitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akka % Test
    val akkaHttpTestkit = "com.typesafe.akka" %% "akka-http-testkit" % Versions.akkaHttp % Test
    val actorTestkitTyped = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akka % Test
    val persistenceTestkit = "com.typesafe.akka" %% "akka-persistence-testkit" % Versions.akka % Test
    val streamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test
  }

  object dynamodb {
    val awsSdk = "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.935"
    val streamKinesisAdapter = "com.amazonaws" % "dynamodb-streams-kinesis-adapter" % "1.5.2"

    //"com.github.j5ik2o" %% "akka-persistence-kafka" % "1.0.14",
    //"com.github.j5ik2o" %% "akka-persistence-dynamodb-journal" % "1.1.6",
    //"com.github.j5ik2o" %% "akka-persistence-dynamodb-snapshot" % "1.1.6",
    //"com.typesafe.akka" %% "akka-persistence-dynamodb" % "1.2.0-RC2"
  }

  object logback {
    val classic = "ch.qos.logback" % "logback-classic" % Versions.logback excludeAll (ExclusionRule("org.slf4j"))
  }

  object slf4j {
    val api = "org.slf4j" % "slf4j-api" % Versions.slf4j
  }

  object testContainer {
    val testContainer = "com.dimafeng" %% "testcontainers-scala-scalatest" % Versions.testContainer
    val kafkaTestContainer = "com.dimafeng" %% "testcontainers-scala-kafka" % Versions.testContainer
  }

}
