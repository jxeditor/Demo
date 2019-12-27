package com.test.flink.batch.hive

import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}
import org.apache.flink.table.catalog.hive.HiveCatalog
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
/**
 * @Author: xs
 * @Date: 2019-12-12 15:09
 * @Description: 离线操作hive
 */
object HiveRWOnTable {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "hdfs")

    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tEnv = TableEnvironment.create(settings)
    val hiveCatalog = new HiveCatalog("test", "default",
      "D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\hive_conf", "1.2.1")

    tEnv.registerCatalog("test", hiveCatalog)
    tEnv.useCatalog("test")
    val src = tEnv.sqlQuery("select * from user_test_orc");

    tEnv.sqlUpdate("insert into user_test_orc values ('test', 0,0,'x','2019-12-12')");
    tEnv.execute("insert into user_test_orc");
  }
}
