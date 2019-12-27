package com.test.spark.stream.wc

import com.test.spark.log.LoggerLevels
import org.apache.spark.streaming.{Seconds, StreamingContext, Time}
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}

object StateFullWordCount {

  //Seq这个批次某个单词的次数
  //Option[Int]：以前的结果

  //分好组的数据
  val updateFunc = (iter: Iterator[(String, Seq[Int], Option[Int])]) => {
    //iter.flatMap(it=>Some(it._2.sum + it._3.getOrElse(0)).map(x=>(it._1,x)))
    //iter.map{case(x,y,z)=>Some(y.sum + z.getOrElse(0)).map(m=>(x, m))}
    //iter.map(t => (t._1, t._2.sum + t._3.getOrElse(0)))
    val result = iter.map {
      case (word, current_count, history_count)
      => (word, current_count.sum + history_count.getOrElse(0))
    }.toList.sortBy(_._2).reverse.toIterator
    result
  }

  def updateFunc2(current: Seq[Int], pre: Option[Int]): Option[Int] = {
    Some(current.sum + pre.getOrElse(0))
  }

  def main(args: Array[String]): Unit = {
    LoggerLevels.setLogLevels()
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")

    val conf = new SparkConf().setAppName("StreamingWordCount").setMaster("local[2]")
    val sc = new SparkContext(conf)
    // StreamingContext

    // updateStateByKey必须设置CheckPoint
    sc.setCheckpointDir("ck")

    val ssc = new StreamingContext(sc, Seconds(5))

    // 接收数据
    val ds = ssc.socketTextStream("hadoop01", 9999)

    // DStream是一个特殊的RDD
    val result = ds.flatMap(_.split(" ")).map((_, 1))
      .updateStateByKey(updateFunc2)
    // .updateStateByKey(updateFunc, new HashPartitioner(sc.defaultParallelism), false)

    result.foreachRDD(x => {
      val t = x.collect().toList.sortBy(_._2).reverse
      println(t)
    })
    ssc.start()
    ssc.awaitTermination()
  }
}
