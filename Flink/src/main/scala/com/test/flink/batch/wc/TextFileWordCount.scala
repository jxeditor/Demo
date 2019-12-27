package com.test.flink.batch.wc

import java.{lang, util}

import org.apache.flink.api.common.functions.{FlatMapFunction, GroupReduceFunction}
import org.apache.flink.api.scala._
import org.apache.flink.util.Collector
import org.json.{JSONArray, JSONObject}

/**
 * @Author: xs
 * @Date: 2019-12-02 11:09
 * @Description:
 */
object TextFileWordCount {
  def main(args: Array[String]): Unit = {

    val env = ExecutionEnvironment.getExecutionEnvironment


    val text = env.readTextFile("D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\wc.txt")
    // val text = env.fromElements("Who's there?", "I think I hear them. Stand, ho! Who's there?")

    val counts = text
      .flatMap(_.split(" "))
      .map((_, 1))
      .groupBy(0)
      //      .reduceGroup(new GroupReduceFunction[(String, Int), (String, Int)] {
      //        override def reduce(iterable: lang.Iterable[(String, Int)], collector: Collector[(String, Int)]): Unit = {
      //          val value = iterable.iterator()
      //          var map = Map[String, Int]()
      //          while (value.hasNext) {
      //            val tuple = value.next()
      //            map += (tuple._1 -> (tuple._2 + map.getOrElse(tuple._1, 0)))
      //          }
      //          map.foreach(x => {
      //            collector.collect((x._1, x._2))
      //          })
      //        }
      //      })
      .reduce((x, y) => (x._1, x._2 + y._2))
    //.sum(1)
    // counts.print()


    val json = env.readTextFile("D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\wc.txt")
    val map = json
      .flatMap(_.split(" "))
      .map(x => {
        val obj = new JSONObject(x)
        val array = obj.getJSONArray("arr")
        var list = Seq[(String, String)]()
        for (i <- 0 until array.length()) {
          val info = array.getJSONObject(i)
          list = list :+ (info.getString("x"), info.getString("y"))
        }
        list
      })
      .flatMap(x => x)

    map.print()
    // counts.writeAsCsv("D:\\工作\\IdeaProjects\\Demo\\Flink\\src\\main\\resources\\output", "\n", " ")
    // env.execute("Scala WordCount Example")
  }

  def test(data: String): Seq[(String, String)] = {
    val array = new JSONArray(data)
    var list = Seq[(String, String)]()
    for (i <- 0 until array.length()) {
      val info = array.getJSONObject(i)
      list = list :+ (info.getString("x"), info.getString("y"))
    }
    list
  }
}
