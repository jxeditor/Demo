package com.test.flink.stream.hive

import java.util.Properties

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.fs.bucketing.{BucketingSink, DateTimeBucketer}
import org.apache.hadoop.conf.Configuration
import org.apache.flink.api.scala._
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.json.JSONObject
import scala.collection.convert.WrapAsJava._

object HiveDemoOnSink {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "hadoop03:9092")
    properties.setProperty("group.id", "test")
    //    properties.setProperty("auto.offset.reset", "latest")
    properties.setProperty("auto.offset.reset", "earliest")
    properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    val consumer010 = new FlinkKafkaConsumer010[String](
      // "test",
      List("test", "test1"),
      new JsonDeserializationSchema(),
      properties
    ).setStartFromEarliest()
    //      .setStartFromLatest()
    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val senv = StreamExecutionEnvironment.getExecutionEnvironment
    senv.enableCheckpointing(60000)
    senv.setStateBackend(new FsStateBackend("hdfs://hadoop01:8020/flink/checkpoints"))
    senv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val conf = senv.getCheckpointConfig
    // conf.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION)
    conf.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    val dataStream = senv.addSource(consumer010)

    val configuration = new Configuration()
    configuration.set("fs.defaultFS", "hdfs://hadoop01:8020")
    val bucketingSink = new BucketingSink[Message]("/user/hive/warehouse/user_test_orc").setBucketer(
      new DateTimeBucketer[Message]("'c_date='yyyy-MM-dd")
    ).setWriter(
      new OrcWriter[Message](classOf[Message])
    ).setBatchSize(1024 * 10).setFSConfig(configuration)
    var temp = 0
    val ds = dataStream.map(data => {
      temp = temp + 1
      val json = new JSONObject(data.toString)
      val topic = json.get("topic").toString
      val partition = json.get("partition").toString.toInt
      val offset = json.get("offset").toString.toInt
      val timestamp = json.get("timestamp").toString.toLong
      new Message(topic, partition, offset, timestamp, json.toString())
    })

    ds.print()

    ds.addSink(bucketingSink)
    senv.execute()
  }


}

/**
 * *
 * MSCK REPAIR TABLE user_test_orc;
 *
 *
 * CREATE TABLE user_test_orc(
 * topic string,
 * partition int,
 * offset int,
 * msg string)
 * partitioned BY (c_date string)
 * ROW FORMAT serde 'org.apache.hadoop.hive.ql.io.orc.OrcSerde'
 * STORED AS inputformat 'org.apache.hadoop.hive.ql.io.orc.OrcInputFormat'
 * outputformat 'org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat';
 *
 *
 *
 */
