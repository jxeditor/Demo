package com.test.flink.stream.hbase.develop

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes

object HBaseDemoOnSink {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(5000)
    val ds = env.socketTextStream("hadoop01", 9999, '\n')
    val wordCounts = ds.flatMap(_.split(" "))
      .map((_, 1))
      .keyBy(0)
      .sum(1).map(x => {
      (x._1, x._2.toString)
    })
    wordCounts.print().setParallelism(1)
    println(wordCounts)

    val dataStream = env.addSource(new RichSourceFunction[(String, String)] {
      var conn: Connection = null
      var table: Table = null
      var scan: Scan = null

      override def open(parameters: Configuration): Unit = {
        val tableName = TableName.valueOf(TABLE_NAME)
        val conf: org.apache.hadoop.conf.Configuration = HBaseConfiguration.create()
        conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
        conf.set("hbase.zookeeper.property.clientPort", "2181")
        conn = ConnectionFactory.createConnection(conf)
        table = conn.getTable(tableName)
        scan = new Scan()
        scan.addFamily(Bytes.toBytes(TABLE_CF))
      }

      override def run(sourceContext: SourceFunction.SourceContext[(String, String)]): Unit = {
        val rs = table.getScanner(scan)
        val iterator = rs.iterator()
        while (iterator.hasNext) {
          val result = iterator.next()
          val rowKey = Bytes.toString(result.getRow)
          val value = Bytes.toString(result.getValue(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count")))
          sourceContext.collect((rowKey, value))
        }
      }

      override def cancel(): Unit = {

      }

      override def close(): Unit = {
        try {
          if (table != null) table.close()
          if (conn != null) conn.close()
        } catch {
          case e: Exception => println(e.getMessage)
        }
      }
    })
    wordCounts.addSink(new RichSinkFunction[(String, String)] {
      var conn: Connection = null
      var table: Table = null
      var mutator: BufferedMutator = null

      override def open(parameters: Configuration): Unit = {
        val tableName = TableName.valueOf(TABLE_NAME)
        val conf: org.apache.hadoop.conf.Configuration = HBaseConfiguration.create()
        conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
        conf.set("hbase.zookeeper.property.clientPort", "2181")
        conn = ConnectionFactory.createConnection(conf)
        table = conn.getTable(tableName)
        mutator = conn.getBufferedMutator(new BufferedMutatorParams(tableName)
          .writeBufferSize(10 * 1024 * 1024)
        )
      }

      override def invoke(value: (String, String), context: SinkFunction.Context[_]): Unit = {
        val time1 = System.currentTimeMillis()
        val put = new Put(Bytes.toBytes(value._1))
        //        put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"), Bytes.toBytes(value._2.toString().replace("---", "")))
        put.addColumn(Bytes.toBytes(TABLE_CF), Bytes.toBytes("count"), Bytes.toBytes(value._2 + "---"))
        mutator.mutate(put)
        mutator.flush()
        val time2 = System.currentTimeMillis()
        println(time2 - time1)
      }

      override def close(): Unit = {
        try {
          if (table != null) table.close()
          if (mutator != null) mutator.close()
          if (conn != null) conn.close()
        } catch {
          case e: Exception => println(e.getMessage)
        }
      }
    })
    env.execute()
  }
}


/**
  create external table hbasedemo(
    key string,
    cl1 string,
    cl2 string)
  row format serde 'org.apache.hadoop.hive.hbase.HBaseSerDe'
  stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
  with serdeproperties(
    'hbase.columns.mapping'=':key,\nF1:M,\nF2:M',
    'serialization.format'='1')
  tblproperties('hbase.table.name'='KYLIN_EKZFKGYDY7');

  */