package com.test.flink.batch.hive

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, SqlDialect, TableUtils}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.catalog.hive.HiveCatalog

/**
  * @author XiaShuai on 2020/4/17.
  */
object RwHiveDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, settings)
    //    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    //    val tEnv = TableEnvironment.create(settings)

    val hiveCatalog = new HiveCatalog("test", "game_ods",
      "F:\\operation_framework_test\\flink\\src\\main\\resources\\hive_conf", "2.1.1")
    hiveCatalog.getHiveConf.set("dfs.client.use.datanode.hostname", "true")
    tEnv.registerCatalog("test", hiveCatalog)
    tEnv.useCatalog("test")

    // 当结果表为Hive表时
    tEnv.getConfig.setSqlDialect(SqlDialect.HIVE)


    tEnv.listTables().foreach(println)
    val table = tEnv.sqlQuery(
      s"""
         |select *
         |from game_ods.event
         |WHERE app='game_skuld_01'
         |AND dt='2019-08-16'
         |AND event='event_app.track_2'
         |limit 1
         |""".stripMargin).dropColumns("id")
    table.printSchema()



    val rows = TableUtils.collectToList(table)
    println(rows)
    tEnv.execute("")
  }
}
