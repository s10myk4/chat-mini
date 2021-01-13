package com.s10myk4.readApiServer

import com.s10myk4.readApiServer.query.{KafkaConnectSettings, MessageTableOperator}
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}

object Main {

  def main(args: Array[String]): Unit = {
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build()
    val tEnv = TableEnvironment.create(settings)

    val kafkaSettings = KafkaConnectSettings(
      kafkaHost = "kafka",
      kafkaPort = 9092,
      zookeeperHost = "zookeeper",
      zookeeperPort = 2181,
      groupId = "akka-persistence-journal" //TODO ??
    )
    new MessageTableOperator(tEnv, kafkaSettings).createTable()
  }

}
