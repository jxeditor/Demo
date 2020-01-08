package com.test.flink.stream.sql

import java.util

import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.descriptors.{Json, Kafka, Schema}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2019-12-12 12:38
 * @Description:
 */
object KafkaSourceExample {
  def main(args: Array[String]): Unit = {
    val map = Map("payload" -> Types.OBJECT_ARRAY(Types.ROW_NAMED(Array("col1", "col2"), Types.INT, Types.STRING)))
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    val kafka = new Kafka()
      .version("0.10")
      .topic("test")
      .property("bootstrap.servers", "hadoop03:9092")
      //      .startFromEarliest()
      .startFromLatest()

    // {"topic":"test","partition":3,"offset":1,"payload":[{"col1":1,"col2":"2"},{"col1":3,"col2":"4"}]}
    tableEnv.connect(kafka)
      .withFormat(
        new Json().failOnMissingField(true).deriveSchema()
      )
      .withSchema(
        registerSchema(map)
      )
      .inAppendMode()
      .registerTableSource("test")


    val sql = "select * from test"
    val table = tableEnv.sqlQuery(sql)

    table.printSchema()

    val value = tableEnv.toAppendStream[Row](table)
    value.print()
    bsEnv.execute("Flink Demo")
  }

  def registerSchema(map: Map[String, TypeInformation[_]]): Schema = {
    val schema = new Schema()
    map.map(x => {
      schema.field(x._1, x._2)
    })
    schema
  }
}
