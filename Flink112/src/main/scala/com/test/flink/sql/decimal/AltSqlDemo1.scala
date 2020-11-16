package com.test.flink.sql.decimal

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment

/**
 *
 * @author tianchen by 2020/10/26
 */
object AltSqlDemo1 {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    bsEnv.setParallelism(1)
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    tableEnv.executeSql(
      s"""
         |create table print_sink(
         |	data array<decimal(3,2)>,
         |  test bigint
         |) with (
         |      'connector' = 'print'
         |)
         |""".stripMargin)
    tableEnv.executeSql(
      s"""
         |insert into print_sink
         |select array[0.40, 0.5, 0.06], 11 & 8
         |""".stripMargin)
  }
}
