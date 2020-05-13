package com.test.spark.batch.hive

import com.test.spark.log.LoggerLevels
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
  * @author XiaShuai on 2020/4/17.
  */
object HiveReadDemo {
  def main(args: Array[String]): Unit = {
//    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "E:\\Soft\\hadoop-2.8.0")
    val conf = new SparkConf().setAppName("StreamWriteToHBase").setMaster("local[*]")
    val session = SparkSession.builder().config(conf)
      .enableHiveSupport()
      .getOrCreate()

    val frame = session.sql(
      s"""
         |select *
         |from game_ods.event
         |WHERE app='game_skuld_01'
         |AND dt='2019-08-16'
         |AND event='event_app.track_2'
         |limit 1
         |""".stripMargin
    )
    frame.show()
    session.stop()
  }
}
