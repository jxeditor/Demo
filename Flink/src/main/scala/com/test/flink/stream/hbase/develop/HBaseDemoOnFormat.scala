package com.test.flink.stream.hbase.develop

//import org.apache.flink.addons.hbase.TableInputFormat

import org.apache.flink.api.common.io.OutputFormat
import org.apache.flink.api.scala._
import org.apache.flink.api.java.tuple.Tuple2
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.api.scala.hadoop.mapreduce.{HadoopInputFormat, HadoopOutputFormat}
import org.apache.flink.configuration.Configuration
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.{TableInputFormat, TableOutputFormat}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hive.ql.io.orc.{OrcNewOutputFormat, OrcOutputFormat}
import org.apache.hadoop.mapreduce.Job

object HBaseDemoOnFormat {
  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
    conf.set("hbase.zookeeper.property.clientPort", "2181")
    conf.set("hbase.defaults.for.version.skip", "true")
    conf.set("mapred.output.dir", "hdfs://hadoop01:8020/demo")
    conf.set(org.apache.hadoop.hbase.mapreduce.TableOutputFormat.OUTPUT_TABLE, "test1")
    conf.set(org.apache.hadoop.hbase.mapreduce.TableInputFormat.INPUT_TABLE, "test")
    conf.setClass("mapreduce.job.outputformat.class",
      classOf[org.apache.hadoop.hbase.mapreduce.TableOutputFormat[String]],
      classOf[org.apache.hadoop.mapreduce.OutputFormat[String, Mutation]])

    val job = Job.getInstance(conf)

    //    val tableInputFormat = new TableInputFormat[Tuple2[String, String]] {
    //
    //      val tuple2 = new Tuple2[String, String]
    //
    //      override def getScanner: Scan = {
    //        scan
    //      }
    //
    //      override def getTableName: String = TABLE_NAME
    //
    //      override def mapResultToTuple(result: Result): Tuple2[String, String] = {
    //        val key = Bytes.toString(result.getRow)
    //        val value = Bytes.toString(result.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count")))
    //        tuple2.setField(key, 0)
    //        tuple2.setField(value, 1)
    //        tuple2
    //      }
    //
    //      override def configure(parameters: Configuration): Unit = {
    //        val tableName = TableName.valueOf(TABLE_NAME)
    //
    //        var conn: Connection = null
    //        val conf: org.apache.hadoop.conf.Configuration = HBaseConfiguration.create()
    //        conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
    //        conf.set("hbase.zookeeper.property.clientPort", "2181")
    //        conn = ConnectionFactory.createConnection(conf)
    //        table = conn.getTable(tableName).asInstanceOf[HTable]
    //        scan = new Scan()
    //        scan.addFamily(Bytes.toBytes(TABLE_CF))
    //      }
    //    }

    val hadoopIF = new HadoopInputFormat(new TableInputFormat(), classOf[ImmutableBytesWritable], classOf[Result], job)
    val hadoopOF = new HadoopOutputFormat[ImmutableBytesWritable, Mutation](new TableOutputFormat(), job)
    val value = env.createInput(hadoopIF)

    val ds = value.map(x => {
      val put = new Put(x._2.getRow)
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
      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l1"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l1")))
      //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2"), x._2.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("l2")))
      (x._1, put.asInstanceOf[Mutation])
      //("1")
    })

    //val format = new OrcOutputFormat[String]()
    ds.output(hadoopOF)
    // ds.output(format.asInstanceOf[OutputFormat[String]])

    //    val tableOuputFormat = new OutputFormat[Tuple2[String, String]] {
    //      var conn: Connection = null
    //
    //      override def configure(configuration: Configuration): Unit = {
    //
    //      }
    //
    //      override def open(i: Int, i1: Int): Unit = {
    //        val conf: org.apache.hadoop.conf.Configuration = HBaseConfiguration.create()
    //        conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
    //        conf.set("hbase.zookeeper.property.clientPort", "2181")
    //        conn = ConnectionFactory.createConnection(conf)
    //      }
    //
    //      override def writeRecord(it: Tuple2[String, String]): Unit = {
    //        val tableName = TableName.valueOf(TABLE_NAME)
    //        val put = new Put(Bytes.toBytes(it.f0))
    //        put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"), Bytes.toBytes(it.f1 + "小猪猪"))
    //        conn.getTable(tableName).put(put)
    //      }
    //
    //      override def close(): Unit = {
    //        try {
    //          if (conn != null) conn.close()
    //        } catch {
    //          case e: Exception => println(e.getMessage)
    //        }
    //      }
    //    }
    //    val hbaseDs = env.createInput(tableInputFormat)

    //    val hadoopOF = new HadoopOutputFormat[String, Mutation](new TableOutputFormat(), job)
    //
    //    println(hbaseDs.collect().length)
    //    val ds = hbaseDs.map(x => {
    //      val put = new Put(x.f0.getBytes())
    //      put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"), Bytes.toBytes(x.f1 + "SIXSS"))
    //      (x.f0, put.asInstanceOf[Mutation])
    //    })
    //    ds.output(hadoopOF)
    env.execute()
  }
}

