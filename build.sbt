import Dependencies._
import sbt.Keys.mainClass

//
val `domain-event-router` = (project in file("domain-event-router"))
  .settings(
    name := "domain-event-router",
    BaseSettings(),
    //mainClass in(Compile, run) := Some("com.s10myk4.chatservice.Main"),
    libraryDependencies ++= Seq(
        akka.actorTyped,
        akka.stream,
        akka.streamKafka,
        akka.serializeJackson,
        dynamodb.awsSdk,
        dynamodb.streamKinesisAdapter
      )
  )

val `write-api-server` = (project in file("write-api-server"))
  .settings(
    name := "write-api-server",
    BaseSettings(),
    mainClass in(Compile, run) := Some("com.s10myk4.chatservice.Main"),
    libraryDependencies ++= Seq(
      logback.classic,
      akka.actorTyped,
      akka.stream,
      akka.serializeJackson,
      akka.slf4j,
      akka.clusterShardingTyped,
      akka.persistenceTyped,
      akka.http,
      akka.httpSprayJson,
      "org.iq80.leveldb" % "leveldb" % "0.9",
      "com.github.j5ik2o" %% "akka-persistence-kafka" % "1.0.14",
      "com.github.j5ik2o" %% "akka-persistence-dynamodb-journal" % "1.1.6",
      "com.github.j5ik2o" %% "akka-persistence-dynamodb-snapshot" % "1.1.6",
      "com.typesafe.akka" %% "akka-persistence-dynamodb" % "1.2.0-RC2",
      //test only
      akka.actorTestkitTyped,
      akka.persistenceTestkit,
      akka.streamTestkit,
      akka.akkaHttpTestkit,
    )
  )

//val `read-api-server` = (project in file("read-api-server"))
//  .enablePlugins(JavaAppPackaging)
//  .settings(
//    name := "read-api-server",
//    mainClass in(Compile, run) := Some("com.s10myk4.readApiServer.query.Main"),
//    mainClass in reStart := Some("com.s10myk4.readApiServer.query.Main"),
//    dockerBaseImage := "flink:1.11.3-scala_2.12",
//    version in Docker := "1.2.0",
//    //maintainer in Docker := ""
//    dockerUpdateLatest := true,
//    dockerEntrypoint := Seq("/opt/docker/bin/chat-mini-read-api-server"),
//    dockerExposedPorts := Seq(8081, 8081),
//    packageName in Docker := s"chat-mini/${name.value}",
//    //bashScriptExtraDefines ++= Seq(
//    //  "addJava -Xms${JVM_HEAP_MIN:-1024m}",
//    //  "addJava -Xmx${JVM_HEAP_MAX:-1024m}",
//    //  "addJava -XX:MaxMetaspaceSize=${JVM_META_MAX:-512M}",
//    //  "addJava ${JVM_GC_OPTIONS:--XX:+UseG1GC}",
//    //  "addJava -Dconfig.resource=${CONFIG_RESOURCE:-application.conf}",
//    //  "addJava -Dakka.remote.startup-timeout=60s"
//    //),
//    //dockerCommands ++= Seq(
//    //  Cmd("USER", "root")
//    //),
//
//    //fork in run := true,
//    //javaOptions in run ++= Seq(
//    //  s"-Dcom.sun.management.jmxremote.port=${sys.env.getOrElse("JMX_PORT", "8999")}",
//    //  "-Dcom.sun.management.jmxremote.authenticate=false",
//    //  "-Dcom.sun.management.jmxremote.ssl=false",
//    //  "-Dcom.sun.management.jmxremote.local.only=false",
//    //  "-Dcom.sun.management.jmxremote"
//    //),
//    //javaOptions in Universal ++= Seq(
//    //  "-Dcom.sun.management.jmxremote",
//    //  "-Dcom.sun.management.jmxremote.local.only=true",
//    //  "-Dcom.sun.management.jmxremote.authenticate=false" //,
//    //),
//  )
//  .settings(
//    libraryDependencies ++= Seq(
//      "org.apache.flink" %% "flink-scala" % "1.11.2",
//      "org.apache.flink" %% "flink-streaming-scala" % "1.11.2" % "provided",
//      "org.apache.flink" % "flink-table" % "1.11.2" % "provided" pomOnly(),
//      "org.apache.flink" %% "flink-table-planner-blink" % "1.11.2" % "provided",
//      "org.apache.flink" %% "flink-table-api-scala-bridge" % "1.11.2",
//      "org.apache.flink" %% "flink-clients" % "1.11.2",
//      "org.apache.kafka" % "kafka-clients" % "2.2.0",
//      "org.apache.flink" %% "flink-sql-connector-kafka" % "1.11.2",
//    )
//  )
//
val `read-model-updater` = (project in file("read-model-updater"))
  .settings(
    name := "read-model-updater",
    BaseSettings()
  )

val root = (project in file("."))
  .settings(
    name := "chat-mini"
    //BaseSettings()
  )
  .aggregate(
    `write-api-server`,
    //`read-api-server`,
    `domain-event-router`,
    `read-model-updater`
  )
