package com.test.flink.stream.dim.hbase

import com.test.flink.hbase.HBaseAsyncLookupTableSource
import com.test.flink.redis.RedisAsyncLookupTableSource
import org.apache.flink.api.common.typeinfo.Types
import org.apache.flink.api.java.io.jdbc.{JDBCAppendTableSink, JDBCOptions, JDBCUpsertTableSink}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.{StreamTableEnvironment, _}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2020-01-03 17:15
 * @Description:
 */
object DoubleStreamHBaseDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val settings = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tEnv = StreamTableEnvironment.create(env, settings)
    val ds = env.socketTextStream("hadoop01", 9999, '\n')
    // 1000,good0c,1566375779658
    val demo = ds.flatMap(_.split(" ")).map(x => {
      val arr = x.split(",")
      Demo(arr(0), arr(1), arr(2))
    })

    tEnv.registerDataStream("user_click_name", demo, 'id, 'user_click, 'time, 'proctime.proctime)

    val redisSource = HBaseAsyncLookupTableSource.Builder.newBuilder()
      .withFieldNames(Array("id", "name", "age"))
      .withFieldTypes(Array(Types.STRING, Types.STRING, Types.STRING))
      .withTableName("user")
      .build()
    tEnv.registerTableSource("info", redisSource)

    val sql =
    //"select t1.id,t1.user_click,t2.name" +
      "select * " +
        " from user_click_name as t1" +
        " join info FOR SYSTEM_TIME AS OF t1.proctime as t2" +
        " on t1.id = t2.id"

    val table = tEnv.sqlQuery(sql)
    val tableName = table.toString
    tEnv.toAppendStream[Row](table).print()


    tEnv.execute("")
  }

  case class Demo(id: String, user_click: String, time: String)

}
