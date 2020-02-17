package com.test.flink.stream.dim.mysql

import java.util.Properties

import com.test.flink.stream.udx.udaf.CollectListUDAF
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.io.jdbc.{JDBCOptions, JDBCTableSource}
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableSchema}
import org.apache.flink.types.Row
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.flink.table.factories.TableSinkFactory
import org.apache.flink.table.planner.functions.aggfunctions.CollectAggFunction
import org.apache.kafka.clients.consumer.ConsumerConfig

import scala.collection.convert.WrapAsJava._

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


    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "cdh04:9092")
    properties.setProperty("group.id", "test")
    properties.setProperty("auto.offset.reset", "latest")
    properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    val consumer010 = new FlinkKafkaConsumer010[String](
      List("test"),
      new SimpleStringSchema(),
      properties
    ).setStartFromLatest()
    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val ds = env.addSource(consumer010)
    //    val ds = env.socketTextStream("cdh04", 9999, '\n')
    val demo: DataStream[Demo] = ds.flatMap(_.split(" ")).map(x => {
      val arr = x.split(",")
      Demo(arr(0).toInt, arr(1))
    })
    val table = tEnv.sqlQuery("SELECT * FROM sessions")

    tEnv.registerDataStream("demoTable", demo, 'user, 'result, 'proctime.proctime)
    tEnv.registerFunction("collect_list", new CollectListUDAF)

    val result = tEnv.sqlQuery("select `a`.`user`,collect_list(`a`.`result`) from demoTable a left join sessions FOR SYSTEM_TIME AS OF a.proctime AS b ON `a`.`user` = `b`.`uid` group by `a`.`user`")
    tEnv.toRetractStream[Row](result).filter(_._1).print
    //    tEnv.toAppendStream[Row](result).print
    tEnv.execute("")
  }

  case class Order(user: Int)

  case class Demo(user: Int, result: String)

}
