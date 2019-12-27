package com.test.spark.stream.sql.udtf

import java.sql.Timestamp

import com.test.spark.log.LoggerLevels
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.functions._

/**
 * @Author: xs
 * @Date: 2019-12-18 15:46
 * @Description:
 */
object SparkUdtfDemo {
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

    session.sql("CREATE TEMPORARY FUNCTION UdtfDemo as 'com.test.spark.stream.sql.udtf.UdtfDemo'")

    val q = word.selectExpr("UdtfDemo(value)").withColumn("",explode($"test"))
      .writeStream
      .queryName("kafka_test")
      .outputMode(OutputMode.Update())
      .format("console")
      .start()
    q.awaitTermination()
  }
}
