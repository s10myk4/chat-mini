package com.s10myk4.readApiServer.query

import org.apache.flink.table.api.{TableEnvironment, TableResult}

final case class KafkaConnectSettings(
                                       kafkaHost: String,
                                       kafkaPort: Int,
                                       zookeeperHost: String,
                                       zookeeperPort: Int,
                                       groupId: String
                                     )

class MessageTableOperator(
                            env: TableEnvironment,
                            settings: KafkaConnectSettings,
                          ) {

  def createTable(): TableResult = {
    val ddl =
      s"""
    CREATE TABLE Messages (
      id         BIGINT,
      sender     BIGINT,
      roomId     BIGINT,
      body       STRING,
      sequenceNo BIGINT
    ) WITH (
      'connector' = 'kafka',
      'topic' = 'Messages',
      'scan.startup.mode' = 'earliest-offset',
      'properties.zookeeper.connect' = '${settings.zookeeperHost}:${settings.zookeeperPort}',
      'properties.bootstrap.servers' = '${settings.kafkaHost}:${settings.kafkaPort}',
      'properties.group.id' = '${settings.groupId}',
      'format' = 'json'
    )""".stripMargin
    env.executeSql(ddl)
  }
}
