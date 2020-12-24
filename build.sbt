lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion = "2.6.10"

lazy val `write-api-server` = (project in file("write-api-server"))
  .settings(BaseSettings())
  .settings(
    name := "write-api-server",
    mainClass in(Compile, run) := Some("com.s10myk4.chatservice.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.github.j5ik2o" %% "akka-persistence-kafka" % "1.0.14",
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    )
  )

lazy val `read-api-server` = (project in file("read-api-server"))
  .settings(name := "read-api-server")

val root = (project in file("."))
  .settings(BaseSettings())
  .settings(name := "chat-mini")
  .aggregate(`write-api-server`, `read-api-server`)

