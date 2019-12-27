package com.test.flink.stream.graph.template

import org.apache.flink.streaming.api.scala.DataStream

trait EndPoint[U] extends Point{
  def process(dataStream: DataStream[U])
}
