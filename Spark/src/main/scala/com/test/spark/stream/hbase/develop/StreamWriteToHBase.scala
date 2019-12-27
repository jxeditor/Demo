package com.test.spark.stream.hbase.develop

import com.test.spark.log.LoggerLevels
import com.test.spark.stream.hbase.util.HBaseUtil
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * @Author: xs
 * @Date: 2019-12-10 09:00
 * @Description:
 */
object StreamWriteToHBase {
  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val conf = new SparkConf().setAppName("StreamWriteToHBase").setMaster("local[*]")
    val hbaseConf = HBaseUtil.getConf()
    val hbaseConn = HBaseUtil.getConn()
    val session = SparkSession.builder().config(conf)
      .getOrCreate()
    val sc = session.sparkContext
    val ssc: StreamingContext = new StreamingContext(sc, Seconds(5))

    // 数据处理
    val ds = ssc.socketTextStream("hadoop01", 9999)
    val result = ds.flatMap(_.split(" "))
      .map((_, 1))
      .reduceByKey(_ + _)

    // 存储入HBase
    result.foreachRDD(rdd => {
      if (!rdd.isEmpty()) {
        HBaseUtil.getTable(hbaseConn, TABLE_NAME)
        val jobConf = HBaseUtil.getNewJobConf(hbaseConf, TABLE_NAME)
        // 先处理消息
        rdd.map(data => {
          val rowKey = data._1.toString
          val put = new Put(rowKey.getBytes())
          put.addColumn(TABLE_CF.getBytes(), "count".getBytes(), data._2.toString.getBytes())
          (new ImmutableBytesWritable(), put)
        }).saveAsNewAPIHadoopDataset(jobConf)
      }
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
