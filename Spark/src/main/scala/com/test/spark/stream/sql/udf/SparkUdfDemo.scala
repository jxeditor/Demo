package com.test.spark.stream.sql.udf

import java.sql.Timestamp

import com.test.spark.log.LoggerLevels
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.OutputMode

/**
 * @Author: xs
 * @Date: 2019-12-18 15:02
 * @Description:
 */
object SparkUdfDemo {
  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val conf = new SparkConf().setAppName("SparkSQLKafkaWordCount").setMaster("local[*]")
    val session = SparkSession.builder().config(conf)
      .getOrCreate()

    val df = session.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "hadoop03:9092")
      .option("subscribe", "test")
      .option("startingOffsets", "earliest")
      //      .option("startingOffsets", "latest")
      .load()

    import session.implicits._
    df.printSchema()

    val word = df.selectExpr("CAST(topic AS STRING)",
      "CAST(value AS STRING)",
      "CAST(partition as INTEGER)",
      "CAST(timestamp as Timestamp)"
    ).as[(String, String, Integer, Timestamp)]

    // 实名函数
    // session.udf.register("upperUdf", upperUdf _)
    // 匿名函数
    session.udf.register("upperUdf", (str: String) => {
      str.toUpperCase
    })

    val q = word.selectExpr("upperUdf(topic)")
      .writeStream
      .queryName("kafka_test")
      .outputMode(OutputMode.Append())
      .format("console")
      .start()
    q.awaitTermination()
  }

  // 实名函数
  def upperUdf(str: String): String = {
    str.toUpperCase
  }
}
