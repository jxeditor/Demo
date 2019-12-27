package com.test.flink.batch.sql

import org.apache.flink.api.scala._
import org.apache.flink.table.api.scala._

/**
 * @Author: xs
 * @Date: 2019-12-03 09:30
 * @Description:
 */
object BatchWordCountSQL {
  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment
    val tEnv = BatchTableEnvironment.create(env)

    val input = env.fromElements(WC("hello", 1), WC("hello", 1), WC("ciao", 1))

    // 注册成表
    tEnv.registerDataSet("WordCount", input, 'word, 'frequency)

    // 查询
    val table = tEnv.sqlQuery("SELECT word, SUM(frequency) FROM WordCount GROUP BY word")

    table.toDataSet[WC].print()
  }

  case class WC(word: String, frequency: Long)

}
