package com.test.flink.stream.window.table

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
    tEnv.connect(new Kafka()
      .version("0.11")
      .topic("test01")
      .property("bootstrap.servers", "cdh04:9092")
      .startFromEarliest())
      .withFormat(new Json().failOnMissingField(false).schema(
        Types.ROW_NAMED(
          Array("business", "database", "es", "sql", "table", "ts", "type"),
          Types.STRING,
          Types.STRING,
          Types.INT,
          Types.STRING,
          Types.STRING,
          Types.LONG,
          Types.STRING
        )
      ))
      .withSchema(
        new Schema()
          .field("business", BasicTypeInfo.STRING_TYPE_INFO)
          .field("database", BasicTypeInfo.STRING_TYPE_INFO)
          .field("es", BasicTypeInfo.INT_TYPE_INFO)
          .field("sql", BasicTypeInfo.STRING_TYPE_INFO)
          .field("table", BasicTypeInfo.STRING_TYPE_INFO)
          //.field("ts", BasicTypeInfo.LONG_TYPE_INFO)
          .field("rowtime", Types.SQL_TIMESTAMP).rowtime(new Rowtime().timestampsFromField("ts").watermarksPeriodicBounded(60000))
          .field("type", BasicTypeInfo.STRING_TYPE_INFO)
          .field("proctime", Types.SQL_TIMESTAMP).proctime()
      )
      .inAppendMode()
      .createTemporaryTable("test01")

    val table = tEnv.from("test01")
      .select('business, 'rowtime, 'es, 'proctime)
      .window(
        Slide over 5.second every 2.second on 'rowtime as 'p)
      .groupBy('p, 'business).aggregate('es.count as 'num)
      .select('business, 'num, 'p.start, 'p.end)
    tEnv.toAppendStream[Row](table).print()
    env.execute()
  }
}
