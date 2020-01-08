package com.test.demand

/**
 * @Author: xs
 * @Date: 2020-01-04 15:23
 * @Description:
 */
object ListDemo {
  def main(args: Array[String]): Unit = {
    val list1 = List(2,3,4,5,6)
    val list2 = List(2,3,4)

    val ints = list1.diff(list2)
    println(ints)
  }
}
