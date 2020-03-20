package com.test.flink.stream

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.table.descriptors.Kafka
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2020-03-19 13:44
 * @Description:
 */
object SocketStreamDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(4)
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, settings)

    val ds = env.socketTextStream("cdh04", 9999, '\n')
      .toTable(tEnv)

    val res1 = tEnv.sqlQuery(s"select *,now() from $ds")
    res1.toAppendStream[Row].print()

    tEnv.execute("test")
  }
}
