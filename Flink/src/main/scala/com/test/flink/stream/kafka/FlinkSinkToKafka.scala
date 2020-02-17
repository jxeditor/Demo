package com.test.flink.stream.kafka

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaConsumer011, FlinkKafkaProducer010}
import org.apache.flink.streaming.api.scala._

/**
 * @Author: xs
 * @Date: 2019-12-30 08:54
 * @Description:
 */
object FlinkSinkToKafka {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "test"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val props = new Properties()
    props.put("bootstrap.servers", "cdh04:9092")
//    props.put("zookeeper.connect", "cdh04:2181")
    props.put("group.id", "demo11")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
//    props.put("enable.auto.commit","true")
//    props.put("auto.offset.reset", "latest")
    // props.put("auto.offset.reset", "earliest")

    val student = env.addSource(new FlinkKafkaConsumer011(
      READ_TOPIC, //这个 kafka topic 需要和上面的工具类的 topic 一致
      new SimpleStringSchema, props).setStartFromLatest())

//    student.addSink(new FlinkKafkaProducer010("hadoop01:9092", "test01", new SimpleStringSchema)).name("test01")
//      .setParallelism(6)

    student.print()

    env.execute("flink learning connectors kafka")
  }
}
