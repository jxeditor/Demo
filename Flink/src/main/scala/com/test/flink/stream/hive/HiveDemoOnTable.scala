package com.test.flink.stream.hive


import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.connectors.hive.HiveTableSink
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}
import org.apache.flink.table.catalog.{CatalogTable, ObjectPath}
import org.apache.flink.table.catalog.hive.HiveCatalog
import org.apache.flink.table.descriptors.Kafka
import org.apache.flink.types.Row
import org.apache.hadoop.hive.metastore.RetryingMetaStoreClient
import org.apache.hadoop.mapred.JobConf

/**
 * @Author: xs
 * @Date: 2019-12-05 10:22
 * @Description:
 */
object HiveDemoOnTable {
  //  def main(args: Array[String]): Unit = {
  //    val env = StreamExecutionEnvironment.getExecutionEnvironment
  //    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
  //
  //    val tEnv = StreamTableEnvironment.create(env, bsSettings)
  //
  //
  //    val ds1 = env.socketTextStream("hadoop01", 9999, '\n')
  //
  //    val hiveCatalog = new HiveCatalog("test", "default",
  //      "D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\hive_conf", "1.2.1")
  //    tEnv.registerCatalog("test", hiveCatalog)
  //    tEnv.useCatalog("test")
  //
  //    val table = tEnv.sqlQuery("select `topic`,`partition`,`offset`,msg,`c_date` from user_test_orc")
  //
  //
  //    table.insertInto("user_test_orc")
  //
  //    env.execute("test")
  //  }
  //
  //  //  case class Order(user: Int, product: String, amount: Int)
  //  case class Order(topic: String, partition: Integer, offset: Integer, msg: String, c_date: String)
  //
  //}

  /**
   * 实测流表不能写入hive,只能使用TableEnvironment
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val stEnv = StreamTableEnvironment.create(env, bsSettings)
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inBatchMode().build()
    val tEnv = TableEnvironment.create(settings)


    val hiveCatalog = new HiveCatalog("test", "default",
      "D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\hive_conf", "1.2.1")
    stEnv.registerCatalog("test", hiveCatalog)
    stEnv.useCatalog("test")

    val ds1 = env.socketTextStream("hadoop01", 9999, '\n')
    val orderA = ds1.flatMap(_.split(" ")).map(x => {
      Order("test", 0, 0, x, "2019-12-05")
    })

    stEnv.registerDataStream("OrderA", orderA, 'topic, 'partition, 'offset, 'msg, 'c_date)
    val OrderA = stEnv.fromDataStream(orderA, 'topic, 'partition, 'offset, 'msg, 'c_date)

    tEnv.sqlUpdate("insert into user_test_orc select 'topic', CAST('partition' AS INT), CAST('offset' AS INT), 'msg', 'c_date' from" + OrderA)
    stEnv.toAppendStream[Row](OrderA).print().setParallelism(1)
    tEnv.execute("")

    env.execute("test")
  }

  case class Order(topic: String, partition: Integer, offset: Integer, msg: String, c_date: String)

}