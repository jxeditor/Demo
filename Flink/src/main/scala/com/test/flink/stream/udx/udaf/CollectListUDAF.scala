package com.test.flink.stream.udx.udaf

import org.apache.flink.table.functions.AggregateFunction

import scala.collection.mutable.ListBuffer

/**
 * @Author: xs
 * @Date: 2020-01-15 14:30
 * @Description:
 */
class CollectListUDAF extends AggregateFunction[String, ListBuffer[String]] {
  override def getValue(accumulator: ListBuffer[String]): String = {
    accumulator.distinct.mkString(",")
  }

  override def createAccumulator(): ListBuffer[String] = new ListBuffer[String]

  def accumulate(accumulator: ListBuffer[String], i: String) = {
    accumulator.append(i)
  }
}
