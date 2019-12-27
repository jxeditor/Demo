package com.test.spark.stream.sql.udaf

import org.apache.spark.sql.Row
import org.apache.spark.sql.expressions.{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{DataType, IntegerType, StructField, StructType}

/**
 * @Author: xs
 * @Date: 2019-12-18 15:47
 * @Description:
 */
class UdafDemo extends UserDefinedAggregateFunction {
  //定义输入数据的类型,两种写法都可以
  // override def inputSchema: StructType = StructType(Array(StructField("input", IntegerType, true)))
  override def inputSchema: StructType = StructType(StructField("input", IntegerType) :: Nil)

  // 定义聚合过程中所处理的数据类型
  override def bufferSchema: StructType = StructType(Array(StructField("cache", IntegerType, nullable = true)))

  // 定义输入数据的类型
  override def dataType: DataType = IntegerType

  // 规定一致性
  override def deterministic: Boolean = true

  // 在聚合之前，每组数据的初始化操作
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer(0) = 0
  }

  // 每组数据中，当新的值进来的时候，如何进行聚合值的计算
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    // 求最大值
    if (input.getInt(0) > buffer.getInt(0))
      buffer(0) = input.getInt(0)
  }

  // 合并各个分组的结果
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    if (buffer2.getInt(0) > buffer1.getInt(0)) {
      buffer1(0) = buffer2.getInt(0)
    }
  }

  // 返回最终结果
  override def evaluate(buffer: Row): Any = {
    buffer.getInt(0)
  }
}
