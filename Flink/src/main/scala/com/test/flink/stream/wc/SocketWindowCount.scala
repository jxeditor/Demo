package com.test.flink.stream.wc

import java.io.{File, FileInputStream}

import org.apache.flink.api.common.io.{FileInputFormat, FilePathFilter}
import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.functions.source.FileProcessingMode
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.descriptors.Schema

/**
 * @Author: xs
 * @Date: 2019-12-03 09:03
 * @Description:
 */
object SocketWindowCount {
  def main(args: Array[String]): Unit = {
    //    val hostname = try {
    //      ParameterTool.fromArgs(args).get("hostname", "localhost")
    //    } catch {
    //      case e: Exception => {
    //        System.err.println("请输入用户名,--hostname <hostname>")
    //        return
    //      }
    //    }
    //
    //    val port = try {
    //      ParameterTool.fromArgs(args).getInt("port")
    //    } catch {
    //      case e: Exception => {
    //        System.err.println("请输入主机名和端口,--hostname <hostname> --port <port>")
    //        return
    //      }
    //    }


    System.setProperty("HADOOP_USER_NAME", "hdfs")
    val senv = StreamExecutionEnvironment.getExecutionEnvironment
    // senv.enableCheckpointing(60000, CheckpointingMode.EXACTLY_ONCE)
    // senv.setStateBackend(new FsStateBackend("hdfs://hadoop01:8020/flink/checkpoints"))
    // senv.getCheckpointConfig.setTolerableCheckpointFailureNumber(10)
    //    val conf = senv.getCheckpointConfig
    //    conf.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION)
    val ds = senv.socketTextStream("eva", 9999, '\n')

    val wordCounts = ds.flatMap(_.split(" "))
      .map(WordWithCount(_, 1))
      .keyBy("word")
      .timeWindow(Time.seconds(5))
      .sum("count")

    wordCounts.print().setParallelism(1)
    println(wordCounts)

    senv.execute("Socket Window WordCount")
  }

  case class WordWithCount(word: String, count: Long)


}

