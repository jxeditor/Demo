package com.test.flink.stream.join

import org.apache.flink.addons.hbase.HBaseTableSource
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._
import org.apache.flink.types.Row

import scala.collection.mutable

/**
  * @author XiaShuai on 2020/4/22.
  */
object TemporalTableDemo {
  def main(args: Array[String]): Unit = {
    // Get the stream and table environments.
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tEnv = StreamTableEnvironment.create(env)

    // Provide a static data set of the rates history table.
    val ratesHistoryData = new mutable.MutableList[(String, Long)]
    ratesHistoryData.+=(("US Dollar", 102L))
    ratesHistoryData.+=(("Euro", 114L))
    ratesHistoryData.+=(("Yen", 1L))
    ratesHistoryData.+=(("Euro", 116L))
    ratesHistoryData.+=(("Euro", 119L))

    // Create and register an example table using above data set.
    // In the real setup, you should replace this with your own table.
    val ratesHistory = env
      .fromCollection(ratesHistoryData)
      .toTable(tEnv, 'r_currency, 'r_rate, 'r_proctime.proctime)

    tEnv.createTemporaryView("RatesHistory", ratesHistory)

    // Create and register TemporalTableFunction.
    // Define "r_proctime" as the time attribute and "r_currency" as the primary key.
    val rates = ratesHistory.createTemporalTableFunction('r_proctime, 'r_currency) // <==== (1)
    tEnv.registerFunction("Rates", rates) // <==== (2)

    //tEnv.sqlQuery("SELECT * FROM Rates();").toAppendStream[Row].print()
    tEnv.sqlQuery("SELECT * FROM RatesHistory").toAppendStream[Row].print()

   tEnv.execute("")

  }
}
