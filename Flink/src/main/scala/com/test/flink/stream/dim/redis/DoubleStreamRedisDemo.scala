package com.test.flink.stream.dim.redis

import com.test.flink.redis.RedisAsyncLookupTableSource
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation, Types}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.api.java.io.jdbc.{JDBCAppendTableSink, JDBCOptions, JDBCUpsertTableSink}
import org.apache.flink.table.api.scala.{StreamTableEnvironment, _}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2020-01-03 17:15
 * @Description:
 */
object DoubleStreamRedisDemo {
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

    val redisSource = RedisAsyncLookupTableSource.Builder.newBuilder().withFieldNames(Array("id", "name", "age"))
      .withFieldTypes(Array(Types.STRING, Types.STRING, Types.STRING))
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
    //    val value2 = tEnv.toRetractStream[Row](table).filter(_._1).map(_._2)

    // ----------------------------------------------------------------------------

    // AppendTableSink
    val sinkA = JDBCAppendTableSink.builder()
      .setDrivername("com.mysql.jdbc.Driver")
      .setDBUrl("jdbc:mysql://localhost:3306/world?autoReconnect=true&failOverReadOnly=false&useSSL=false")
      .setUsername("root")
      .setPassword("123456")
      .setQuery("insert into test (uid) values (?)")
      .setBatchSize(1)
      .setParameterTypes(Types.STRING)
      .build()
    //    tEnv.registerTableSink("jdbcOutputTable", Array[String]("uid"), Array[TypeInformation[_]](BasicTypeInfo.STRING_TYPE_INFO), sinkA)


    // UpsertTableSink
    val sinkB = JDBCUpsertTableSink.builder()
      .setOptions(JDBCOptions.builder()
        .setDriverName("com.mysql.jdbc.Driver")
        .setDBUrl("jdbc:mysql://localhost:3306/world?autoReconnect=true&failOverReadOnly=false&useSSL=false")
        .setUsername("root")
        .setPassword("123456")
        .setTableName("test")
        .build())
      .setTableSchema(TableSchema.builder()
        .field("uid", DataTypes.STRING())
        .build())
      .setFlushIntervalMills(1)
      .build()
    // tEnv.registerTableSink("jdbcOutputTable", sinkB)

    // table.insertInto("jdbcOutputTable")
    //    val insertSQL = "insert into jdbcOutputTable (uid) select id from " + tableName
    //    tEnv.sqlUpdate(insertSQL)
    tEnv.execute("")
  }

  case class Demo(id: String, user_click: String, time: String)

}
