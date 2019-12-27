package com.test.spark.stream.kafka

import java.sql.Timestamp

import com.test.spark.log.LoggerLevels
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.sql.functions._
import org.json.{JSONArray, JSONObject}

/**
 * @Author: xs
 * @Date: 2019-12-11 16:10
 * @Description:
 */
object SparkSQLKafkaWordCount {
  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val conf = new SparkConf().setAppName("SparkSQLKafkaWordCount").setMaster("local[*]")
    conf.set("spark.sql.streaming.checkpointLocation", "E:\\ck")
    val session = SparkSession.builder().config(conf)
      .getOrCreate()


    val df = session.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "hadoop03:9092")
      .option("subscribe", "test")
      //      .option("startingOffsets", "earliest")
      .option("startingOffsets", "latest")
      .load()

    import session.implicits._
    df.printSchema()

    val word = df.selectExpr("CAST(key AS STRING)",
      "CAST(value AS STRING)",
      "CAST(partition as INTEGER)",
      "CAST(timestamp as Timestamp)"
    ).as[(String, String, Integer, Timestamp)]
    //.select("value").as[String].flatMap(_.split(" "))


    // 水印处理迟到10分钟之内的数据
    val wordcount = word.withWatermark(
      "timestamp", "10 minutes")
      // 每一分钟对10分钟窗口进行分组
      .groupBy(
        window(col("timestamp"), "10 minutes", "1 minutes"),
        col("value"))
      .count()

    // val wordcount = word.groupBy("value").count()

    //    val q = df.select("*")
    //      .writeStream
    //      .queryName("kafka_test")
    //      .outputMode(OutputMode.Append())
    //      .format("console")
    //      .start()
    //    q.awaitTermination()


    val q = wordcount.select("*")
      .writeStream
      .queryName("kafka_test")
      .outputMode(OutputMode.Complete())
      .format("kafka")
      .option("kafka.bootstrap.servers", "hadoop03:9092")
      .option("topic", "test1")
      //      .option("startingOffsets", "earliest")
      .trigger(Trigger.ProcessingTime(300))
      .start()
    q.awaitTermination()

  }

  def list2Str(list: Seq[String]): String = {
    val obj = new JSONObject()
    obj.put("grade_id", list.head.split(",")(0))
    val arr = new JSONArray()
    for (tuple <- list) {
      val array = tuple.split("")
      val temp = new JSONObject()
      temp.put("subject_id", array(1))
      temp.put("type_id", array(2))
      temp.put("source", array(3))
      arr.put(temp)
    }
    obj.put("subject_type", arr)
    obj.toString()
  }
}

