package com.test.flink.stream.window.sql

import com.test.flink.commons.CreateDDL
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.scala._
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation, Types}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, Slide, Tumble}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.descriptors.{Json, Kafka, Rowtime, Schema}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2019-12-17 14:19
 * @Description:
 */
object KafkaWindowDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(4)
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, settings)

    // {"business":"sdasf","database":"sqweqr","es":2314,"sql":"","table":"t_cash_loan","ts":1576050001925,"type":"UPDATE"}
    val sql = CreateDDL.createKafkaTable()
    tEnv.sqlUpdate(sql)

    val query =
      """
        | SELECT business,COUNT(1) as pv,
        | TUMBLE_START(rowtime, INTERVAL '5' second) as t_start,
        | TUMBLE_END(rowtime, INTERVAL '5' second) as t_end
        | FROM test
        | GROUP BY business,TUMBLE(rowtime, INTERVAL '5' second)
            """.stripMargin
    val res1 = tEnv.sqlQuery(query)
    res1.toAppendStream[Row].print()
    tEnv.execute("")
  }
}
