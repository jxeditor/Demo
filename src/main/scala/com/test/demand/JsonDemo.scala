package com.test.demand

import java.util

import org.json.{JSONArray, JSONObject}

/**
 * @Author: xs
 * @Date: 2019-12-26 14:23
 * @Description:
 */
object JsonDemo {
  def main(args: Array[String]): Unit = {
    val shuffle = true
    if (shuffle) println(1)
    else println(2)
    val id = "1"
    val temp = new JSONObject()
    var index = -1
    temp.put("id", "3")
    var flag = true
    val arr = new JSONArray("[{'id':'1'},{'id':'2'}]")
    for (i <- 0 until arr.length() if flag) {
      val obj = arr.getJSONObject(i)
      if (obj.getString("id").equals(id)) {
        flag = false
        index = i
      }
    }
    if (!flag)
//      arr.remove(index)
    arr.put(temp)
    println(arr.toString)

    val map = new util.HashMap[String, Int]()
    map.put("s", 1)


    val nObject = new JSONObject("{'id':1}")
    println(nObject.getString("id").toInt)
  }
}
