import java.nio.ByteBuffer
import java.util.Properties

import com.alibaba.fastjson.JSON
import org.apache.avro.reflect.ReflectData
import org.apache.avro.specific.SpecificData
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.core.io.SimpleVersionedSerializer
import org.apache.flink.orc.vector.Vectorizer
import org.apache.flink.orc.writer.OrcBulkWriterFactory
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.SimpleVersionedStringSerializer
import org.apache.flink.streaming.api.functions.sink.filesystem.{BucketAssigner, StreamingFileSink}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, LocalFileSystem, Path}
import org.apache.hadoop.hive.ql.exec.vector.{BytesColumnVector, VectorizedRowBatch}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.orc.OrcFile.WriterOptions
import org.apache.orc.Writer
import org.apache.orc.impl.WriterImpl


/**
  * @author XiaShuai on 2020/6/5.
  */
object OrcFileWriteDemo {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "game_log_game_skuld_01"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(10000L, CheckpointingMode.EXACTLY_ONCE)
    env.setStateBackend(new FsStateBackend("file:///job/flink/ck/Orc"))
    val props = new Properties()
    props.put("bootstrap.servers", "skuldcdhtest1.ktcs:9092")
    props.put("group.id", "xs_test1")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

    val producerProps = new Properties()
    producerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "skuldcdhtest1.ktcs:9092")
    producerProps.setProperty(ProducerConfig.RETRIES_CONFIG, "3")
    // 如果下面配置的是exactly-once的语义 这里必须配置为all
    producerProps.setProperty(ProducerConfig.ACKS_CONFIG, "all")


    val student = env.addSource(new FlinkKafkaConsumer(
      READ_TOPIC, //这个 kafka topic 需要和上面的工具类的 topic 一致
      new SimpleStringSchema, props).setStartFromEarliest()
    ).map(x => {
      val obj = JSON.parseObject(x)
      val data = obj.getJSONObject("param_data")
      data.getString("platform_s") + "," + data.getString("category_s") + "," + data.getString("app_version_code_s")
      Demo(data.getString("platform_s"), data.getString("category_s"), data.getString("app_version_code_s"))
    }).setParallelism(1)

    println(ReflectData.get.getSchema(classOf[Demo]).toString)
    val schema: String = "struct<platform:string,event:string,dt:string>"
    val writerProperties: Properties = new Properties()
    writerProperties.setProperty("orc.compress", "LZ4")

    val writerFactory = new OrcBulkWriterFactory(new DemoVectorizer(schema), writerProperties, new Configuration())
    import org.apache.flink.core.fs.Path
    val sink = StreamingFileSink.forBulkFormat(new Path("F:\\test\\Demo\\Flink11\\src\\main\\resources"),
      writerFactory
    ).build()

    student.addSink(sink).setParallelism(1)
    env.execute("write hdfs")
  }
}

class DemoVectorizer(schema: String) extends Vectorizer[Demo](schema: String) {
  override def vectorize(t: Demo, vectorizedRowBatch: VectorizedRowBatch): Unit = {
    for (i <- 0 until 3) {
      val vector = vectorizedRowBatch.cols(i).asInstanceOf[BytesColumnVector]
      val bytes = t.platform.getBytes
      vector.setVal(i, bytes, 0, bytes.length)
    }
  }
}

case class Demo(platform: String, event: String, dt: String)
