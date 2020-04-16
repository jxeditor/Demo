package com.test.flink.stream.kafka

import java.lang
import java.util.Properties

import com.alibaba.fastjson.JSON
import org.apache.flink.api.common.serialization.{SerializationSchema, SimpleStringSchema}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer010, FlinkKafkaConsumer011, FlinkKafkaProducer010, FlinkKafkaProducer011, KafkaSerializationSchema}
import org.apache.flink.streaming.api.scala._
import org.apache.kafka.clients.producer.ProducerRecord

/**
  * @Author: xs
  * @Date: 2019-12-30 08:54
  * @Description:
  */
object FlinkSinkToKafka {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "game_log_game_skuld_01"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val props = new Properties()
    props.put("bootstrap.servers", "skuldcdhtest1.ktcs:9092")
    props.put("group.id", "xs_test")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    //    props.put("enable.auto.commit","true")
    //    props.put("auto.offset.reset", "latest")
    // props.put("auto.offset.reset", "earliest")

    val student = env.addSource(new FlinkKafkaConsumer011(
      READ_TOPIC, //这个 kafka topic 需要和上面的工具类的 topic 一致
      new SimpleStringSchema, props).setStartFromLatest()
    ).map(x => {
      val obj = JSON.parseObject(x)
      val data = obj.getJSONObject("param_data")
      data.getString("platform_s") + "," + data.getString("category_s") + "," + data.getString("app_version_code_s")
    })

    // student.addSink(new FlinkKafkaProducer010("hadoop01:9092", "test01", new SimpleStringSchema)).name("test01")
    student.addSink(new FlinkKafkaProducer010("skuldcdhtest1.ktcs:9092", "test01", new CustomKafkaSerializationSchema))

    //      .setParallelism(6)

    student.print()

    env.execute("flink learning connectors kafka")
  }
}

// kafka-topics --delete --zookeeper skuldcdhtest1.ktcs:2181 --topic flink_test
class CustomKafkaSerializationSchema extends KafkaSerializationSchema[String] with SerializationSchema[String] {
  override def serialize(element: String, timestamp: java.lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    val arr = element.split(",")
    val topic = arr(0)
    val key = arr(1).getBytes
    val value = element.getBytes
    new ProducerRecord[Array[Byte], Array[Byte]](topic, key, value)
  }

  override def serialize(t: String): Array[Byte] = {
    serialize(t, System.currentTimeMillis())
    null
  }
}