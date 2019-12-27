package com.test.flink.stream.hive


import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.types.Row
import java.sql.{Connection, DriverManager, PreparedStatement, Types}

import org.apache.flink.configuration.Configuration
import org.apache.flink.connectors.hive.HiveTableSink
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.table.catalog.{CatalogTable, ObjectPath}
import org.apache.hadoop.mapred.JobConf

/**
 * @Author: xs
 * @Date: 2019-12-12 15:02
 * @Description:
 */
object HiveDemoOnJDBC {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, bsSettings)
    val ds1 = env.socketTextStream("eva", 9999, '\n')
    val orderA = ds1.flatMap(_.split(" ")).map(x => {
      ("test", 0, 0, x, "2019-12-05")
      //      (0, x, 0)
    })

    orderA.addSink(new RichSinkFunction[(String, Int, Int, String, String)] {
      var conn: Connection = _
      var sql = "insert into table default.users(id,name,age) values(?,?,?)"
      var ppst: PreparedStatement = _

      override def open(parameters: Configuration): Unit = {
        val driverName = "com.mysql.jdbc.Driver"
        Class.forName(driverName);
        conn = DriverManager.getConnection( //
          "jdbc:mysql://localhost:3306/world", //
          "root",
          "123456" //
        )
        ppst = conn.prepareStatement("insert into test (uid) values (?)")
      }

      override def invoke(value: (String, Int, Int, String, String), context: SinkFunction.Context[_]): Unit = {

        ppst.setInt(1, value._3.toInt)
        ppst.executeUpdate

      }

      override def close(): Unit = {
        ppst.close()

        conn.close()
      }
    })

    orderA.print()

    env.execute("test")
  }

  case class Order(topic: String, partition: Integer, offset: Integer, msg: String, c_date: String)

}
