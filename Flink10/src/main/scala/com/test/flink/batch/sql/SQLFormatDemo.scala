package com.test.flink.batch.sql

import com.test.flink.commons.CreateDDL
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

    val mysqlTable = CreateDDL.createMysqlTable()
    tableEnv.sqlUpdate(mysqlTable)

//    val kafkaTable = createKafkaTable()
//    tableEnv.sqlUpdate(kafkaTable)

    tableEnv.toRetractStream[Row](tableEnv.sqlQuery("select uid,listagg(cast(u_score as string)) from test group by uid")).print()

    tableEnv.sqlUpdate("drop table test")

    tableEnv.execute("")
  }
}
