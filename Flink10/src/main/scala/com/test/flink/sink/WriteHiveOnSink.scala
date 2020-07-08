package com.test.flink.sink

import java.util
import java.util.Properties

import com.alibaba.fastjson.JSON
import com.test.flink.CustomParquetAvroWriters
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.api.common.serialization.{SimpleStringEncoder, SimpleStringSchema}
import org.apache.flink.core.fs.Path
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy
import org.apache.flink.streaming.api.functions.sink.filesystem.{BucketAssigner, StreamingFileSink}
import org.apache.flink.streaming.api.scala._
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.parquet.hadoop.metadata.CompressionCodecName


/**
  * @author XiaShuai on 2020/4/24.
  */
object WriteHiveOnSink {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "test"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(5000L, CheckpointingMode.EXACTLY_ONCE)
    val props = new Properties()
    props.put("bootstrap.servers", "skuldcdhtest1.ktcs:9092")
    props.put("group.id", "xs_test1")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    //    props.put("enable.auto.commit","true")
    //    props.put("auto.offset.reset", "latest")
    //    props.put("auto.offset.reset", "earliest")

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
      Demo(data.getString("platform_s"), data.getString("category_s"), 100000000L)
    })


    //    val sink = StreamingFileSink.forRowFormat(new Path("F:\\test\\Demo\\Flink10\\src\\main\\resources"), new SimpleStringEncoder[Demo]())
    //      .withBucketAssigner(new BucketAssigner[Demo, String] {
    //        override def getBucketId(in: Demo, context: BucketAssigner.Context): String = {
    //          in.platform + "/" + in.event
    //        }
    //
    //        override def getSerializer: SimpleVersionedSerializer[String] = {
    //          SimpleVersionedStringSerializer.INSTANCE
    //        }
    //      })
    //      .build()
    //    student.addSink(sink)

    val sink = StreamingFileSink
      .forBulkFormat(new Path("F:\\test\\Demo\\Flink10\\src\\main\\resources"), CustomParquetAvroWriters.forReflectRecord(classOf[Demo], CompressionCodecName.SNAPPY))
      .withBucketAssigner(new BucketAssigner[Demo, String] {
        override def getBucketId(element: Demo, context: BucketAssigner.Context): String = {
          s"platform=${element.platform}/event=${element.event}"
        }

        override def getSerializer: SimpleVersionedSerializer[String] = {
          SimpleVersionedStringSerializer.INSTANCE
        }
      })
      .build()


    student.addSink(sink)
    env.execute("write hdfs")
  }
}

case class Demo(platform: String, event: String, dt: Long)
