package com.test.flink.stream.dim.mysql

import org.apache.flink.api.java.io.jdbc.{JDBCOptions, JDBCTableSource}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.{StreamTableEnvironment, _}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.types.Row

/**
 * 动态 join
 */
object FlinkStremaJDBC {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val setting = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tEnv = StreamTableEnvironment.create(env, setting)
    val jdbcOptions = JDBCOptions.builder()
      .setDBUrl("jdbc:mysql://localhost:3306/world?autoReconnect=true&failOverReadOnly=false&useSSL=false")
      .setUsername("root")
      .setPassword("123456")
      .setTableName("test1")
      .build()

    val tableSchema = TableSchema.builder()
      .field("id", DataTypes.INT())
      .field("score", DataTypes.DOUBLE())
      .build()

    val jdbcTableScource = JDBCTableSource.builder().setOptions(jdbcOptions).setSchema(tableSchema).build()
    tEnv.registerTableSource("session", jdbcTableScource)

    val data = env.socketTextStream("eva", 9999, '\n')
    val demo = data.flatMap(_.split(" ")).map(x => {
      Demo(x.toInt)
    })


    tEnv.registerDataStream("demoTable", demo, 'uid, 'proctime.proctime)

    val result = tEnv.sqlQuery(
      "select a.uid,b.score from demoTable a left join session FOR SYSTEM_TIME AS OF a.proctime AS b on `a`.`uid`=`b`.`id`")
    // tEnv.toRetractStream[Row](result).print()
    tEnv.toAppendStream[Row](result).print()
    tEnv.execute("FlinkStremaJDBC")

  }

  case class Demo(uid: Int)

  case class Info(id: Int, score: Double)

}
