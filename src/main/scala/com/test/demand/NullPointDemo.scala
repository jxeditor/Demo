package com.test.demand

/**
 * @Author: xs
 * @Date: 2019-12-30 09:32
 * @Description:
 */
object NullPointDemo {
  def main(args: Array[String]): Unit = {
    val tuple = null
    val tuple1 = tuple.asInstanceOf[(String, String)]

    println(tuple1)
    println(tuple1._1 == null)
  }
}
