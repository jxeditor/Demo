package com.test.flink.stream.hbase.develop

import org.apache.flink.api.common.io.OutputFormat
import org.apache.flink.api.scala._
import org.apache.flink.api.scala.hadoop.mapreduce.{HadoopInputFormat, HadoopOutputFormat}
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Mutation, Put, Result}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.TableOutputFormat
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapreduce.Job

object HbaseStreamDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
//    env.enableCheckpointing(5000,CheckpointingMode.EXACTLY_ONCE)
    env.setBufferTimeout(0)

    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
    conf.set("hbase.zookeeper.property.clientPort", "2181")
    conf.set("hbase.defaults.for.version.skip", "true")
    conf.set("mapred.output.dir", "hdfs://hadoop01:8020/demo")
    conf.set(org.apache.hadoop.hbase.mapreduce.TableOutputFormat.OUTPUT_TABLE, "test1")
    // conf.set(org.apache.hadoop.hbase.mapreduce.TableInputFormat.INPUT_TABLE, "test")
    conf.setClass("mapreduce.job.outputformat.class",
      classOf[org.apache.hadoop.hbase.mapreduce.TableOutputFormat[String]],
      classOf[org.apache.hadoop.mapreduce.OutputFormat[String, Mutation]])

    val job = Job.getInstance(conf)
    val hadoopOF = new HadoopOutputFormat[String, Mutation](new TableOutputFormat(), job)

    val input = env.socketTextStream("hadoop01", 9999, '\n')
    val wordCounts = input.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(0)
      .timeWindowAll(Time.seconds(5))
      .sum(1).map(x => {
      (x._1, x._2.toString)
    })
    val ds = wordCounts.map(x => {
      val put = new Put(Bytes.toBytes(x._1))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("sex"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("age"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("user"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("context"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l1"))
      //      x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2"))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l1"), Bytes.toBytes("运⾏EFK的每个节点需要消耗很⼤的CPU和内存， 请保证每台虚拟机⾄少分配了4G内存"))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2"), Bytes.toBytes("运⾏EFK的每个节点需要消耗很⼤的CPU和内存， 请保证每台虚拟机⾄少分配了4G内存"))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("sex"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("sex")))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("age"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("age")))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("user"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("user")))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count")))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("context"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("context")))
      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l1"), Bytes.toBytes(x._2))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2")))
      (x._1, put.asInstanceOf[Mutation])
    })
    ds.print()
    ds.writeUsingOutputFormat(hadoopOF)
    env.execute()
  }
}
