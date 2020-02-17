package com.test.flink.stream.sql

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2020-01-08 16:42
 * @Description:
 */
object HBaseSQLExample {
  def main(args: Array[String]): Unit = {
    // System.setProperty("HADOOP_USER_NAME", "hdfs")

    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)


    val sql = "create table test (" +
      "`name` string," +
      "`info` ROW<name varchar, age varchar>" +
      ") with (" +
      " 'connector.type' = 'hbase', " +
      " 'connector.version' = '1.4.3', " +
      " 'connector.table-name' = 'user', " +
      " 'connector.zookeeper.quorum' = 'cdh04:2181', " +
      " 'connector.zookeeper.znode.parent' = '/hbase', " +
      " 'connector.write.buffer-flush.max-size' = '1mb', " +
      " 'connector.write.buffer-flush.max-rows' = '1', " +
      " 'connector.write.buffer-flush.interval' = '2s' " +
      ")"

    tableEnv.sqlUpdate(sql)

    tableEnv.toAppendStream[Row](tableEnv.sqlQuery("select * from test")).print()

    tableEnv.execute("")
  }
}
