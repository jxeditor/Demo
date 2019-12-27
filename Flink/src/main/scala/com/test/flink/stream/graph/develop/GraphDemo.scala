package com.test.flink.stream.graph.develop

import java.util.Properties

import com.test.flink.stream.graph.develop.endpoint.{AEndPoint, BEndPoint}
import com.test.flink.stream.graph.develop.execpoint.{AExecPoint, BExecPoint, CExecPoint}
import com.test.flink.stream.graph.template.Graph
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010
import org.json.JSONObject
import scala.collection.convert.WrapAsJava._

object GraphDemo {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "hadoop03:9092")
    properties.setProperty("group.id", "test")
    properties.setProperty("auto.offset.reset", "earliest")

    val consumer010 = new FlinkKafkaConsumer010[String](
      List("test", "test1"),
      // "test",
      new JsonDeserializationSchema(),
      properties
    ).setStartFromEarliest()

    val senv = StreamExecutionEnvironment.getExecutionEnvironment
    senv.enableCheckpointing(500)

    val dataStream = senv.addSource[String](consumer010)

    val graph = Graph.draw[String](dataStream)

    graph.addPoint("1", new AExecPoint)
    graph.addPoint("2", new BExecPoint("2"))
    graph.addPoint("3", new CExecPoint("3"))
    graph.addPoint("4", new AEndPoint("4"))
    graph.addPoint("5", new BEndPoint("5"))

    graph.addEdge("1", "2", x => x.asInstanceOf[JSONObject].get("topic").equals("test"))
    graph.addEdge("2", "4", x => true)
    graph.addEdge("1", "3", x => x.asInstanceOf[JSONObject].get("topic").equals("test1"))
    graph.addEdge("3", "5", x => true)
    //    graph.addEdge("1", "2", x => x.asInstanceOf[JSONObject].has("value"))
    //    graph.addEdge("1", "3", x => !x.asInstanceOf[JSONObject].has("value"))


    graph.finish()

    senv.execute()
  }
}
