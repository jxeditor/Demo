import java.util
import java.util.{ArrayList, Properties}

import com.alibaba.fastjson.JSON
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.{JsonProperties, Schema}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.java.typeutils.TupleTypeInfo
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.flink.core.fs.Path
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.avro.Schema._
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo


/**
  * @author XiaShuai on 2020/6/5.
  */
object ParquetFileWriteDemo {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "game_log_game_skuld_01"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(60000L, CheckpointingMode.EXACTLY_ONCE)
    env.setStateBackend(new FsStateBackend("file:///job/flink/ck/Orc"))
    val settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tEnv = StreamTableEnvironment.create(env, settings)

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
      Demo(data.getString("platform_s"), data.getString("category_s"), data.getString("app_version_code_s"))
      val fields = new util.ArrayList[Schema.Field]
      fields.add(new Schema.Field("platform", create(org.apache.avro.Schema.Type.STRING), "platform", null))
      fields.add(new Schema.Field("event", create(org.apache.avro.Schema.Type.STRING), "event", null))
      fields.add(new Schema.Field("dt", create(org.apache.avro.Schema.Type.STRING), "dt", null))
      val parquetSinkSchema: Schema = createRecord(fields)
      val record = new GenericData.Record(parquetSinkSchema)
      record.put("platform", data.getString("platform_s"))
      record.put("event", data.getString("category_s"))
      record.put("dt", data.getString("app_version_code_s"))
      record
    }).setParallelism(1)

    val fields = new util.ArrayList[Schema.Field]
    fields.add(new Schema.Field("platform", create(org.apache.avro.Schema.Type.STRING), "platform", null))
    fields.add(new Schema.Field("event", create(org.apache.avro.Schema.Type.STRING), "event", null))
    fields.add(new Schema.Field("dt", create(org.apache.avro.Schema.Type.STRING), "dt", null))
    val parquetSinkSchema: Schema = createRecord(fields)

    //    student.print()
    val tupleTypeInfo = new GenericRecordAvroTypeInfo(parquetSinkSchema)

    val table = tEnv.fromDataStream(student)

    val stream = tEnv.toAppendStream(table)(tupleTypeInfo)

    val writerFactory = ParquetAvroWriters.forGenericRecord(parquetSinkSchema)

    val sink: StreamingFileSink[GenericRecord] = StreamingFileSink.forBulkFormat(new Path("F:\\test\\Demo\\Flink11\\src\\main\\resources"),
      writerFactory
    ).build()


    stream.addSink(sink).setParallelism(1)
    env.execute("write hdfs")
  }
}

