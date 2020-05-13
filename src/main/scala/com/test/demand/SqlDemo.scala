package com.test.demand

/**
  * @author XiaShuai on 2020/5/12.
  */
object SqlDemo {
  def main(args: Array[String]): Unit = {
    val key = "name"


    val test = Array("a")

    val value = test.filter(_.equals("a"))
    var result = s"$key IN ("
    var connect = ","
    for (i <- value.indices) {
      if (i == value.length - 1) connect = ")"
      result = s"$result '${value(i)}' $connect"
    }
    result = s"$result"
    println(result)

  }
}
