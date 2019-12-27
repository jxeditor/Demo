package com.test.flink.stream.graph.develop.endpoint

import com.test.flink.stream.graph.template.EndPoint
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.DataStream

class BEndPoint(name: String) extends EndPoint[String]{

  override def process(dataStream: DataStream[String]): Unit = {
    dataStream.map(x => "BBBBBEndPoint ===>"+x).print()
  }
}
