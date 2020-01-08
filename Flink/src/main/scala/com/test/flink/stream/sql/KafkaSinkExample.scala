package com.test.flink.stream.sql

import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.descriptors.{Json, Kafka, Schema}
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._

/**
 * @Author: xs
 * @Date: 2020-01-06 10:53
 * @Description:
 */
object KafkaSinkExample {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    val topic = "test"

    val ds = bsEnv.socketTextStream("hadoop01", 9999, '\n')
    val source = ds.flatMap(_.split(" ")).map(x => {
      Source(x.toInt, "test")
    })

    tableEnv
      .connect(
        new Kafka()
          .version("0.10")
          .topic(topic)
          .property("zookeeper.connect", "hadoop03:2181")
          .property("bootstrap.servers", "hadoop03:9092"))
      .withFormat(
        new Json().deriveSchema)
      .withSchema(
        new Schema()
          .field("user", Types.INT)
          .field("result", Types.STRING)
      )
      .inAppendMode
      .registerTableSink(topic)


    tableEnv.registerDataStream("demoTable", source, 'user, 'result, 'proctime.proctime)

    val sql = "insert into " + topic + " select user, `result` from demoTable"

    tableEnv.sqlUpdate(sql)
    tableEnv.execute("test")
  }

  case class Source(user: Int, result: String)
}
