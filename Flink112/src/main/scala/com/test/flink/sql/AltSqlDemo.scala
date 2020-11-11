package com.test.flink.sql

import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment

/**
 *
 * @author tianchen by 2020/10/26
 */
object AltSqlDemo {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    bsEnv.enableCheckpointing(10000)
    bsEnv.setParallelism(1)
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    tableEnv.executeSql(
      s"""
         |create table print_sink(
         |	data array<decimal(3,2)>
         |) with (
         |      'connector' = 'print'
         |)
         |""".stripMargin)
    tableEnv.executeSql(
      s"""
         |insert into print_sink
         |select array[0.4, 0.5, 0.06]
         |""".stripMargin)
  }
}
