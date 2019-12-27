package com.test.flink.stream.graph.template

import org.apache.flink.streaming.api.scala.DataStream

trait ExecPoint[U, Z] extends Point {
  def process(dataStream: DataStream[U]): DataStream[Z]
}