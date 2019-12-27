package com.test.flink.stream.hbase.develop

import com.test.flink.stream.hbase.util.HbaseUtil
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.hadoop.hbase.client.Put
import org.apache.flink.streaming.api.scala._

object HbaseDemoOnUtil {
  def main(args: Array[String]): Unit = {
    val senv = StreamExecutionEnvironment.getExecutionEnvironment
    senv.enableCheckpointing(500)
    val ds = senv.socketTextStream("hadoop01", 9999, '\n')

    val wordCounts = ds.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(0)
      .sum(1)

    wordCounts.print().setParallelism(1)
    println(wordCounts)
    wordCounts.map(x => {
      val put = new Put((x._1).getBytes)
      put.addColumn("info".getBytes, "count".getBytes, x._2.toString.getBytes)
      HbaseUtil.put("test.demo_", put)
    })

    senv.execute("Write To HBase")
  }
}
