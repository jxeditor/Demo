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

    val source = session.sql(
      s"""
         |select p,id,platform,channel,region,server,data_unix,uid,rid,did from xs_test.event_test where app='h5_12mlcs' and dt ='2020-06-09' and event = 'event_role.activity_4'
         |""".stripMargin).repartition(1)

    source.createOrReplaceTempView("temp")

    val frame = session.sql(
      s"""
         |insert overwrite table xs_test.event_sink partition(app='h5_12mlcs',dt ='2020-06-09',event = 'event_role.activity_4')
         |select * from temp
         |""".stripMargin
    )

    frame.show()
    session.stop()
  }
}
