import sbt.Keys.mainClass

lazy val akkaHttpVersion = "10.2.2"
lazy val akkaVersion = "2.6.10"

lazy val `domain-event-router` = (project in file("domain-event-router"))
  .settings(BaseSettings())
  .settings(
    name := "domain-event-router",
    //mainClass in(Compile, run) := Some("com.s10myk4.chatservice.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.6",
      "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.935",
      "com.amazonaws" % "dynamodb-streams-kinesis-adapter" % "1.5.2"
    )
  )

lazy val `write-api-server` = (project in file("write-api-server"))
  .settings(BaseSettings())
  .settings(
    name := "write-api-server",
    mainClass in(Compile, run) := Some("com.s10myk4.chatservice.Main"),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "org.iq80.leveldb" % "leveldb" % "0.9",
      "com.github.j5ik2o" %% "akka-persistence-kafka" % "1.0.14",
      "com.github.j5ik2o" %% "akka-persistence-dynamodb-journal" % "1.1.6",
      "com.github.j5ik2o" %% "akka-persistence-dynamodb-snapshot" % "1.1.6",
      "com.typesafe.akka" %% "akka-persistence-dynamodb" % "1.2.0-RC2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
    )
  )

lazy val `read-api-server` = (project in file("read-api-server"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "read-api-server",
    mainClass in(Compile, run) := Some("com.s10myk4.readApiServer.query.Main"),
    mainClass in reStart := Some("com.s10myk4.readApiServer.query.Main"),
    dockerBaseImage := "flink:1.11.3-scala_2.12",
    version in Docker := "1.2.0",
    //maintainer in Docker := ""
    dockerUpdateLatest := true,
    dockerEntrypoint := Seq("/opt/docker/bin/chat-mini-read-api-server"),
    dockerExposedPorts := Seq(8081, 8081),
    packageName in Docker := s"chat-mini/${name.value}",
    //bashScriptExtraDefines ++= Seq(
    //  "addJava -Xms${JVM_HEAP_MIN:-1024m}",
    //  "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
    //  "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
    //  "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
    //  "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
    //  "addJava -Dakka.remote.startup-timeout=60s"
    //),
    //dockerCommands ++= Seq(
    //  Cmd("USER", "root")
    //),

    //fork in run := true,
    //javaOptions in run ++= Seq(
    //  s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
    //  "-Dcom.sun.management.jmxremote.authenticate=false",
    //  "-Dcom.sun.management.jmxremote.ssl=false",
    //  "-Dcom.sun.management.jmxremote.local.only=false",
    //  "-Dcom.sun.management.jmxremote"
    //),
    //javaOptions in Universal ++= Seq(
    //  "-Dcom.sun.management.jmxremote",
    //  "-Dcom.sun.management.jmxremote.local.only=true",
    //  "-Dcom.sun.management.jmxremote.authenticate=false" //,
    //),
  )
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-scala" % "1.11.2",
      "org.apache.flink" %% "flink-streaming-scala" % "1.11.2" % "provided",
      "org.apache.flink" % "flink-table" % "1.11.2" % "provided" pomOnly(),
      "org.apache.flink" %% "flink-table-planner-blink" % "1.11.2" % "provided",
      "org.apache.flink" %% "flink-table-api-scala-bridge" % "1.11.2",
      "org.apache.flink" %% "flink-clients" % "1.11.2",
      "org.apache.kafka" % "kafka-clients" % "2.2.0",
      "org.apache.flink" %% "flink-sql-connector-kafka" % "1.11.2",
    )
  )

val root = (project in file("."))
  .settings(BaseSettings())
  .settings(name := "chat-mini")
  .aggregate(`write-api-server`, `read-api-server`, `domain-event-router`)

