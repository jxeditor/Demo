package com.test.flink.sink

import java.util
import java.util.Properties

import com.alibaba.fastjson.JSON
import org.apache.avro.Schema
import org.apache.avro.Schema._
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.core.fs.Path
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.{BucketAssigner, StreamingFileSink}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.types.Row


/**
  * @author XiaShuai on 2020/6/5.
  */
object ParquetFileWriteDemo {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "game_log_game_skuld_01"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(60000L)
    env.setStateBackend(new FsStateBackend("file:///job/flink/ck/Orc"))
    env.setMaxParallelism(4)
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, settings)
    val ckConfig = env.getCheckpointConfig
    ckConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    ckConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)

    val props = new Properties()
    props.put("bootstrap.servers", "skuldcdhtest1.ktcs:9092")
    props.put("group.id", "xs_test3")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

    val student = env.addSource(new FlinkKafkaConsumer(
      READ_TOPIC, //这个 kafka topic 需要和上面的工具类的 topic 一致
      new SimpleStringSchema, props).setStartFromLatest()
    ).map(x => {
      val obj = JSON.parseObject(x)
      val data = obj.getJSONObject("param_data")
      data.getString("platform_s") + "," + data.getString("category_s") + "," + data.getString("app_version_code_s")
      val fields = new util.ArrayList[Schema.Field]
      fields.add(new Schema.Field("platform", create(org.apache.avro.Schema.Type.STRING), "platform", null))
      fields.add(new Schema.Field("event", create(org.apache.avro.Schema.Type.STRING), "event", null))
      fields.add(new Schema.Field("dt", create(org.apache.avro.Schema.Type.STRING), "dt", null))
      val parquetSinkSchema: Schema = createRecord("pi", "flinkParquetSink",
        "flink.parquet", true, fields)
      val record = new GenericData.Record(parquetSinkSchema).asInstanceOf[GenericRecord]
      record.put("platform", data.getString("platform_s"))
      record.put("event", data.getString("category_s"))
      record.put("dt", data.getString("app_version_code_s"))
      record
    })

    val table = tEnv.fromDataStream(student)

    //    student.print().setParallelism(1)


    val fields = new util.ArrayList[Schema.Field]
    fields.add(new Schema.Field("platform", create(org.apache.avro.Schema.Type.STRING), "platform", null))
    fields.add(new Schema.Field("event", create(org.apache.avro.Schema.Type.STRING), "event", null))
    fields.add(new Schema.Field("dt", create(org.apache.avro.Schema.Type.STRING), "dt", null))
    val parquetSinkSchema: Schema = createRecord("pi", "flinkParquetSink",
      "flink.parquet", true, fields)


    val writerFactory = ParquetAvroWriters.forGenericRecord(parquetSinkSchema)

    val sink: StreamingFileSink[GenericRecord] = StreamingFileSink.forBulkFormat(new Path("F:\\test\\Demo\\Flink11\\src\\main\\resources"),
      writerFactory
    )
      .withBucketAssigner(new BucketAssigner[GenericRecord, String] {
        override def getBucketId(in: GenericRecord, context: BucketAssigner.Context): String = {
          "part"
        }

        override def getSerializer: SimpleVersionedSerializer[String] = {
          SimpleVersionedStringSerializer.INSTANCE
        }
      })
      .withBucketCheckInterval(60000L)
      .build()
    val tupleTypeInfo = new GenericRecordAvroTypeInfo(parquetSinkSchema)

    val result = tEnv.toAppendStream(table)(tupleTypeInfo)

    result.print()
    env.execute("write hdfs")
  }
}

