import java.nio.ByteBuffer
import java.util.Properties

import com.alibaba.fastjson.JSON
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.orc.writer.OrcBulkWriterFactory
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.hadoop.conf.Configuration
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.flink.core.fs.Path


/**
  * @author XiaShuai on 2020/6/5.
  */
object OrcFileWriteDemo {
  def main(args: Array[String]): Unit = {
    val READ_TOPIC = "game_log_game_skuld_01"
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(60000L, CheckpointingMode.EXACTLY_ONCE)
    env.setStateBackend(new FsStateBackend("file:///job/flink/ck/Orc"))
    val props = new Properties()
    props.put("bootstrap.servers", "skuldcdhtest1.ktcs:9092")
    props.put("group.id", "xs_test3")
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

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
      Demo(data.getString("platform_s"), data.getString("category_s"), data.getString("app_version_code_s"))
    }).setParallelism(1)

    val schema: String = "struct<platform:string,event:string,dt:string>"
    val writerProperties: Properties = new Properties()
    writerProperties.setProperty("orc.compress", "ZLIB")

    val vectorizer = new DemoVectorizer(schema)
    val writerFactory = new OrcBulkWriterFactory(vectorizer, writerProperties, new Configuration())
    val sink = StreamingFileSink.forBulkFormat(new Path("F:\\test\\Demo\\Flink11\\src\\main\\resources"),
      writerFactory
    ).build()

    student.addSink(sink).setParallelism(1)
    env.execute("write hdfs")
  }
}


case class Demo(platform: String, event: String, dt: String)
