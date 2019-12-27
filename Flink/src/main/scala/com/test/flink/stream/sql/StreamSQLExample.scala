package com.test.flink.stream.sql

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2019-12-03 09:59
 * @Description:
 */
object StreamSQLExample {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tEnv = StreamTableEnvironment.create(env)

    val ds1 = env.socketTextStream("hadoop01", 9999, '\n')
    val ds2 = env.socketTextStream("hadoop01", 9998, '\n')


    val orderA: DataStream[Order] = ds1.flatMap(_.split(" ")).map(x => {
      Order(1L, x, 4)
    })

    val orderB: DataStream[Order] = ds2.flatMap(_.split(" ")).map(x => {
      Order(2L, x, 1)
    })

    // 转换为表
    val tableA = tEnv.fromDataStream(orderA, 'user, 'product, 'amount)

    // 注册为表
    tEnv.registerDataStream("OrderB", orderB, 'user, 'product, 'amount)

    // 合并表
    //        val result = tEnv.sqlQuery(
    //          s"""
    //             |SELECT * FROM $tableA WHERE amount > 2
    //             |UNION ALL
    //             |SELECT * FROM OrderB WHERE amount < 2
    //            """.stripMargin)
    // 排序不让使用
    //    val result = tEnv.sqlQuery(
    //                s"""
    //                   SELECT * FROM $tableA ORDER BY amount
    //                  """.stripMargin)
    val result = tEnv.sqlQuery(
      s"""
         |SELECT user,product,amount as amount FROM $tableA
         |UNION
         |SELECT user,product,amount as amount FROM OrderB
            """.stripMargin)

    //    result.toAppendStream[Order].print()
    result.toRetractStream[Order].filter(_._1).print()

    tEnv.toAppendStream[Order](result)
    env.execute()
  }

  case class Order(user: Long, product: String, amount: Int)

}
