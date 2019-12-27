package com.test.flink.stream.es.develop

import java.util
import java.util.Properties

import com.test.flink.stream.es.entry.{JsonDeserializationSchema, TestElasticsearchSinkFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.apache.http.HttpHost
import org.json.JSONObject


object EsDemo {
  def main(args: Array[String]): Unit = {

    //    val ESConfig = new util.HashMap[String, String]()
    //    ESConfig.put("cluster.name", CLUSTER_NAME)
    //    ESConfig.put("bulk.flush.max.actions", MAX_ACTION)

    val addressList = new java.util.ArrayList[HttpHost]()
    addressList.add(new HttpHost(ES_NAME, ES_PORT))

    val senv = StreamExecutionEnvironment.getExecutionEnvironment
    senv.enableCheckpointing(500)

    // val ds = senv.socketTextStream("hadoop03", 10000, '\n')

    val pro = new Properties()
    pro.put("bootstrap.servers", BROKER)
    pro.put("group.id", GROUP_ID)
    pro.put("zookeeper.connect", KAFKA_ZOOKEEPER)


    val topics = TOPIC.split(",")
    val list = new util.ArrayList[String]()
    topics.foreach(list.add(_))
    val topicMsgSchame = new JsonDeserializationSchema
    val Consumer010 = new FlinkKafkaConsumer010[String](
      list, topicMsgSchame, pro
    ).setStartFromEarliest().setCommitOffsetsOnCheckpoints(false)
    val kafkaStream = senv.addSource(Consumer010)

    val kafkaDs = kafkaStream.map(x => {
      val result = new JSONObject(x)
      (result.getString("value"), 1)
    })
    //    val wordCounts = ds.flatMap(_.split(" "))
    //      .map((_, 1))
    //      .keyBy(0)
    //      .sum(1)
    //    wordCounts.print().setParallelism(1)
    //    println(wordCounts)
    //    wordCounts.addSink(new ElasticsearchSink.Builder[(String, Int)](addressList, new TestElasticsearchSinkFunction).build())

    //    hbaseDs.print().setParallelism(1)
    println(kafkaDs)
    kafkaDs.addSink(new ElasticsearchSink.Builder[(String, Int)](addressList, new TestElasticsearchSinkFunction).build())
    senv.execute("Write To ES T")

  }
}
