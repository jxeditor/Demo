package com.test.flink.stream.hive

import com.test.flink.stream.mysql.JDBCDemo.Order
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.api.java.io.jdbc.JDBCAppendTableSink
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, Types}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2019-12-16 09:09
 * @Description:
 */
object HiveDemoOnJDBCSink {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val settings = EnvironmentSettings.newInstance()
      .useBlinkPlanner()
      .inStreamingMode()
      .build()
    val tEnv = StreamTableEnvironment.create(env, settings)

    val ds1 = env.socketTextStream("hadoop01", 9999, '\n')
    val orderA = ds1.flatMap(_.split(" ")).map(x => {
      Row.of(x, x, x)
    })


    val FIELD_TYPES = Array[TypeInformation[_]](
      BasicTypeInfo.STRING_TYPE_INFO,
      BasicTypeInfo.STRING_TYPE_INFO,
      BasicTypeInfo.STRING_TYPE_INFO
    )

    val sinkA: JDBCAppendTableSink = JDBCAppendTableSink.builder()
      .setDrivername("org.apache.hive.jdbc.HiveDriver")
      .setDBUrl("jdbc:hive2://hadoop01:10000/default")
      .setUsername("hdfs")
      .setPassword("hdfs")
      .setQuery("insert into table default.users(id,name,age) values(?,?,?)")
      .setBatchSize(1)
      .setParameterTypes(FIELD_TYPES: _ *)
      .build()
    val stream = orderA.javaStream
    stream.print()
    sinkA.emitDataStream(stream)

    tEnv.execute("")
  }
}
