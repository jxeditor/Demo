package com.test.spark.batch.es

import org.apache.spark.sql.SparkSession
import org.elasticsearch.spark._
import org.elasticsearch.spark.sql._

/**
 * @Author: xs
 * @Date: 2019-12-06 15:23
 * @Description:
 */
object WriteToEsDemo {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "E:\\Soft\\hadoop-2.8.0")
    val spark: SparkSession = SparkSession.builder().appName("WriteToEsDemo")
      .master("local[*]")
      .config("es.index.auto.create", "true")
      .config("es.nodes", "skuldcdhtest1.ktcs:9200,skuldcdhtest2.ktcs:9200,skuldcdhtest3.ktcs:9200")
      .enableHiveSupport()
      .getOrCreate()

    // EsSparkSQL.esDF(spark,"/test")

    val df = spark.sql(
      s"""
         |select p,id,platform,channel,region,server,data_unix,uid,rid,did from game_ods.event where app='game_skuld_01' and dt ='2020-06-09' and event = 'event_role.activity_4'
         |""".stripMargin)

//    df.show(1)StreamTask
    df.saveToEs("rs_offline_test",Map("es.mapping.id" -> "id"))
  }

  case class User(id: String, user_name: String, user_age: String)

}
