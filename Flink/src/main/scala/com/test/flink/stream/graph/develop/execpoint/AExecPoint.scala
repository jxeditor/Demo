package com.test.flink.stream.graph.develop.execpoint

import com.test.flink.stream.graph.template.ExecPoint
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala.DataStream
import org.json.JSONObject

class AExecPoint extends ExecPoint[String, JSONObject]{

  override def process(dataStream: DataStream[String]): DataStream[JSONObject] = {
    dataStream.map(x => {
      new JSONObject(x)
    })
  }
}
