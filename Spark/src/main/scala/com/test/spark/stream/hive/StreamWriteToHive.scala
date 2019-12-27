package com.test.spark.stream.hive

import com.test.spark.log.LoggerLevels
import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * @Author: xs
 * @Date: 2019-12-10 09:06
 * @Description:
 */
object StreamWriteToHive {
  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val conf = new SparkConf().setAppName("StreamWriteToHBase").setMaster("local[*]")

    val session = SparkSession.builder().config(conf)
      .enableHiveSupport()
      .getOrCreate()
    val sc = session.sparkContext
    val ssc: StreamingContext = new StreamingContext(sc, Seconds(5))

    // DStream->DFrame
    val ds = ssc.socketTextStream("hadoop01", 9999)
    val result = ds.flatMap(_.split(" "))
      .map((_, 1))
      .reduceByKey(_ + _)
    result.foreachRDD(rdd => {
      import session.sqlContext.implicits._
      rdd.toDF("name", "count").createOrReplaceTempView("test")
      val dataframe = session.sql("select * from  test ")
      dataframe.show()
    })
    // DFrame
    // val df = session.sql("")
    // df.write.mode(SaveMode.Overwrite).insertInto("test")

    ssc.start()
    ssc.awaitTermination()
  }
}
