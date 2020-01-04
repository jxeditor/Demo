package com.test.flink.stream.mysql

import org.apache.flink.api.java.io.jdbc.{JDBCOptions, JDBCTableSource}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.table.api.scala.{StreamTableEnvironment, _}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2019-12-24 13:41
 * @Description: 流表join
 */
object DoubleStreamJDBCDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val settings = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tEnv = StreamTableEnvironment.create(env, settings)
    val jdbcOptions = JDBCOptions.builder()
      .setDriverName("com.mysql.jdbc.Driver")
      .setDBUrl("jdbc:mysql://localhost:3306/world?autoReconnect=true&failOverReadOnly=false&useSSL=false")
      .setUsername("root")
      .setPassword("123456")
      .setTableName("test")
      .build()
    val tableSchema = TableSchema.builder()
      .field("uid", DataTypes.INT())
      .build()
    val jdbcTableSource = JDBCTableSource.builder.setOptions(jdbcOptions).setSchema(tableSchema).build
    tEnv.registerTableSource("sessions", jdbcTableSource)

    val ds = env.socketTextStream("eva", 9999, '\n')
    val demo: DataStream[Demo] = ds.flatMap(_.split(" ")).map(x => {
      Demo(x.toInt, "test")
    })
    val table = tEnv.sqlQuery("SELECT * FROM sessions")

    tEnv.registerDataStream("demoTable", demo, 'user, 'result, 'proctime.proctime)

    val result = tEnv.sqlQuery("select * from demoTable a left join sessions FOR SYSTEM_TIME AS OF a.proctime AS b ON `a`.`user` = `b`.`uid`")
    tEnv.toRetractStream[Row](result).print
    tEnv.toAppendStream[Order](table).print
    tEnv.execute("")
  }

  case class Order(user: Int)

  case class Demo(user: Int, result: String)

}

