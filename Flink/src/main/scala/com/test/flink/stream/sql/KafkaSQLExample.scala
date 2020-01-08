package com.test.flink.stream.sql

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.types.Row
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._

/**
 * @Author: xs
 * @Date: 2020-01-08 17:11
 * @Description:
 */
object KafkaSQLExample {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)


    val sql = "create table test (" +
      "`business` varchar," +
      "`ts` bigint" +
      ") with (" +
      " 'connector.type' = 'kafka', " +
      " 'connector.version' = '0.10', " +
      " 'connector.topic' = 'test', " +
      " 'update-mode' = 'append', " +
      " 'connector.properties.0.key' = 'zookeeper.connect', " +
      " 'connector.properties.0.value' = 'hadoop01:2181', " +
      " 'connector.properties.1.key' = 'bootstrap.servers', " +
      " 'connector.properties.1.value' = 'hadoop01:9092', " +
      " 'connector.properties.2.key' = 'group.id', " +
      " 'connector.properties.2.value' = 'kafkasql', " +
      //      " 'connector.startup-mode' = 'earliest-offset', " +
      " 'connector.startup-mode' = 'latest-offset', " +
      " 'format.type' = 'json', " +
      " 'format.derive-schema' = 'true' " +
      ")"

    tableEnv.sqlUpdate(sql)

    tableEnv.toAppendStream[Row](tableEnv.sqlQuery("select * from test")).print()

    tableEnv.execute("")
  }
}
