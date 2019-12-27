package com.test.spark.stream.kafka

import com.test.spark.log.LoggerLevels
import org.apache.commons.codec.StringDecoder
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * @Author: xs
 * @Date: 2019-12-06 14:36
 * @Description:
 */
object DirectKafkaWordCount {
  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val conf = new SparkConf().setAppName("DirectKafkaWordCount").setMaster("local[*]")
    //    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    //    conf.set("spark.streaming.kafka.consumer.poll.ms", "60000")
    //    conf.set("spark.streaming.kafka.maxRatePerPartition", "500")

    val session = SparkSession.builder().config(conf)
      .getOrCreate()
    val sc = session.sparkContext
    val ssc: StreamingContext = new StreamingContext(sc, Seconds(5))
    val topicsSet = Array("test")
    // val topicsSet = "test".split(",").toSet
    val kafkaParams = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "hadoop03:9092",
      ConsumerConfig.GROUP_ID_CONFIG -> "group_test",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[StringDeserializer],
      //      "key.deserializer" -> classOf[StringDeserializer],
      //      "value.deserializer" -> classOf[StringDeserializer],
      //      // "auto.offset.reset" -> "latest", // latest
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest ", // earliest
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (true: java.lang.Boolean)
    )

    val messages = KafkaUtils.createDirectStream[String, String](ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topicsSet, kafkaParams))

    //    messages.foreachRDD(rdd => {
    //      if (!rdd.isEmpty()) {
    //        // 获取偏移量
    //        val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
    //
    //        // 数据处理
    //
    //        // 提交偏移量
    //        messages.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    //      }
    //    })

    val lines = messages.map(_.value())
    val words = lines.flatMap(_.split(" "))
    val wordCounts = words.map(x => (x, 1L)).reduceByKey(_ + _)
    wordCounts.print()

    ssc.start()
    ssc.awaitTermination()
  }
}
