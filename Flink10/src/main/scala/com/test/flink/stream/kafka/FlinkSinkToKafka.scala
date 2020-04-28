package com.test.flink.stream.kafka

import java.lang
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Properties

import com.alibaba.fastjson.JSON
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.metrics.MeterView
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer, KafkaSerializationSchema}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}


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

    val producerProps = new Properties()
    producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "skuldcdhtest1.ktcs:9092")
    producerProps.setProperty(ProducerConfig.RETRIES_CONFIG, "3")
    // 如果下面配置的是exactly-once的语义 这里必须配置为all
    producerProps.setProperty(ProducerConfig.ACKS_CONFIG, "all")


    val student = env.addSource(new FlinkKafkaConsumer(
      READ_TOPIC, //这个 kafka topic 需要和上面的工具类的 topic 一致
      new SimpleStringSchema, props).setStartFromLatest()
    ).map(x => {
      val obj = JSON.parseObject(x)
      val data = obj.getJSONObject("param_data")
      data.getString("platform_s") + "," + data.getString("category_s") + "," + data.getString("app_version_code_s")
    })

    // student.addSink(new FlinkKafkaProducer010("hadoop01:9092", "test01", new SimpleStringSchema)).name("test01")
    student.addSink(new FlinkKafkaProducer("skuldcdhtest1.ktcs:9092", "test01", new CustomKafkaSerializationSchema))

    val producer = new FlinkKafkaProducer[String]("",
      new MyKafkaSerializationSchema,
      producerProps,
      FlinkKafkaProducer.Semantic.EXACTLY_ONCE)
    producer.setLogFailuresOnly(false)

    student.addSink(producer)

    student.print()

    env.execute("flink learning connectors kafka")
  }
}

// kafka-topics --delete --zookeeper skuldcdhtest1.ktcs:2181 --topic flink_test
class CustomKafkaSerializationSchema extends KeyedSerializationSchema[String] {

  private lazy val CHARSET = Charset.forName("UTF-8")

  override def serializeKey(element: String): Array[Byte] = {
    val arr = element.split(",")
    arr(1).getBytes(CHARSET)
  }

  override def serializeValue(element: String): Array[Byte] = element.getBytes(CHARSET)

  override def getTargetTopic(element: String): String = {
    val arr = element.split(",")
    arr(0)
  }
}


class MyKafkaSerializationSchema extends KafkaSerializationSchema[String] {
  override def serialize(element: String, timestamp: lang.Long): ProducerRecord[Array[Byte], Array[Byte]] = {
    val arr = element.split(",")
    val topic = arr(0)
    val key = arr(1)
    new ProducerRecord[Array[Byte], Array[Byte]](
      topic, key.getBytes, element.getBytes(StandardCharsets.UTF_8)
    )
  }
}