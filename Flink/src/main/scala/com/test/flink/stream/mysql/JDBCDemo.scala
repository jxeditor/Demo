package com.test.flink.stream.mysql

import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation, Types}
import org.apache.flink.api.java.io.jdbc.JDBCAppendTableSink
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

/**
 * @Author: xs
 * @Date: 2019-12-15 13:41
 * @Description:
 * 使用JDBCAppendTableSink注意BatchSize的设置,以及scala使用emitDataStream时需要用到javaStream转换
 */
object JDBCDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val settings = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tEnv = StreamTableEnvironment.create(env, settings)
    //    val orderA: DataStream[Order] = env.fromCollection(Seq(
    //      //      Order(1L, "beer", 3),
    //      //      Order(1L, "diaper", 4),
    //      //      Order(3L, "rubber", 2)))
    //      Order(1L),
    //      Order(2L),
    //      Order(3L)))

    val ds1 = env.socketTextStream("eva", 9999, '\n')
    val orderA = ds1.flatMap(_.split(" ")).map(x => {
      Order(x.toInt)
    })

    val tableA = tEnv.fromDataStream(orderA, 'uid)

    val sinkA: JDBCAppendTableSink = JDBCAppendTableSink.builder()
      .setDrivername("com.mysql.jdbc.Driver")
      .setDBUrl("jdbc:mysql://localhost:3306/world?autoReconnect=true&failOverReadOnly=false&useSSL=false")
      .setUsername("root")
      .setPassword("123456")
      .setQuery("insert into test (uid) values (?)")
      .setBatchSize(1)
      .setParameterTypes(Types.LONG)
      .build()

    tEnv.registerTableSink("jdbcOutputTable", Array[String]("uid",""), Array[TypeInformation[_]](BasicTypeInfo.LONG_TYPE_INFO), sinkA)

    //    val stream = tableA.javaStream
    //    stream.print()
    //    sinkA.emitDataStream(stream)
    tableA.insertInto("jdbcOutputTable")

    tEnv.execute("")
  }

  //  case class Order(user: Long, product: String, amount: Int)
  case class Order(user: Long)

}

