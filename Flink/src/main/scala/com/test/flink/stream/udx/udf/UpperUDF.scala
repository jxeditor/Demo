package com.test.flink.stream.udx.udf

import org.apache.flink.table.functions.ScalarFunction

/**
 * @Author: xs
 * @Date: 2020-03-19 13:36
 * @Description:
 */
class UpperUDF extends ScalarFunction {
  def eval(s: String): String = {
    s.toUpperCase
  }
}
