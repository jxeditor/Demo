package com.test.flink.sql

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.types.Row
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
/**
 * @Author: xs
 * @Date: 2020-03-10 09:57
 * @Description:
 */
object SQLFormatDemo {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    val sql = "create table test (" +
      "`uid` INTEGER," +
      "`u_score` Timestamp(3)" + // MySQL中使用DateTime,FlinkSQL中可以使用timestamp(3)来接收
      ") with (" +
      " 'connector.type' = 'jdbc', " +
      " 'connector.url' = 'jdbc:mysql://localhost:3306/world', " +
      " 'connector.table' = 'test', " +
      " 'connector.driver' = 'com.mysql.jdbc.Driver', " +
      " 'connector.username' = 'root', " +
      " 'connector.password' = '123456'" +
      ")"

    tableEnv.sqlUpdate(sql)

    tableEnv.toRetractStream[Row](tableEnv.sqlQuery("select uid,listagg(cast(u_score as string)) from test group by uid")).print()

    tableEnv.execute("")
  }
}
