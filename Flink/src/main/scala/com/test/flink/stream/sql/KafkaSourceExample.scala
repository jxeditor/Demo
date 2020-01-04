package com.test.flink.stream.sql

import java.util

import com.test.flink.DemoSchema
import com.test.flink.stream.sql.StreamSQLExample.Order
import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.scala._
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
    val stringToString = new util.HashMap[String, String]()
    //    val map = Map("payload" -> Types.OBJECT_ARRAY(Types.ROW_NAMED(Array("1", "2"), Types.INT, Types.STRING)))
    val map = Map("payload" -> Types.STRING)
    stringToString.put("payload", "STRING")
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    val kafka = new Kafka()
      .version("0.10")
      .topic("test")
      .property("bootstrap.servers", "hadoop03:9092")
      // .property("zookeeper.connect", "node2.hadoop:2181")
      //      .startFromEarliest()
      .startFromLatest()

    // {"topic":"test","partition":3,"offset":1,"payload":"测试"}
    // {"topic":"test","partition":3,"offset":1,"payload":[{"1":1,"2":"2"}]}
    // {"topic":"test","partition":3,"offset":1,"payload":[1,2]}
    // {"topic":"test","partition":3,"offset":1,"payload":{"1":1,"2":"2"}}
    tableEnv.connect(kafka)
      .withFormat(
        new Json().failOnMissingField(true).deriveSchema()
      )
      .withSchema(
        //        new DemoSchema().field(stringToString)
        registerSchema(map)
        //        new Schema()
        //          //          .field("user_id", Types.INT)
        //          //          .field("item_id", Types.INT)
        //          //          .field("category_id", Types.INT)
        //          //          .field("behavior", Types.STRING)
        //          .field("payload", Types.STRING)
      )
      .inAppendMode()
      .registerTableSource("test")


    //    val sql = "select count(*) from test"
    val sql = "select * from test"
    val table = tableEnv.sqlQuery(sql)

    table.printSchema()

    val value = tableEnv.toAppendStream[Row](table)
    //    val value = tableEnv.toRetractStream[Row](table)
    //    value.filter(_._1).print()

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
