package com.test.spark.stream.wc

import com.test.spark.log.LoggerLevels
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}
import org.apache.spark.{SparkConf, SparkContext}

object SocketWordCount {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    LoggerLevels.setLogLevels()

    val conf = new SparkConf().setAppName("SocketWordCount").setMaster("local[2]")
    val sc = new SparkContext(conf)
    // StreamingContext
    val ssc = new StreamingContext(sc, Seconds(5))

    // 接收数据
    val ds = ssc.socketTextStream("hadoop01", 9999)

    // DStream是一个特殊的RDD
    val result = ds.flatMap(_.split(" "))
      .map((_, 1))
      .reduceByKey(_ + _)
    // 存储数据--这里简单打印
    result.print()
    ssc.start()
    // 等待结束
    ssc.awaitTermination()
  }
}
