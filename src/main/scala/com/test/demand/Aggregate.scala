package com.test.demand

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util
import java.util.concurrent.atomic.LongAdder
import java.util.function.Function
import org.json.JSONObject


/**
 * @Author: xs
 * @Date: 2019-12-02 13:23
 * @Description: 对数据进行处理
 */
object Aggregate {
  def main(args: Array[String]): Unit = {
    // input jid,course_id,difficult
    val value1 = "100001078_1.36,100001097_1.54,100001103_1.55,100001110_1.39,100001117_1.55,100001127_1.5,100001138_1.49,100001991_1.46,100002011_1.8,100002041_1.45,100002465_1.42,100002468_1.45"
    val value2 = "100001103_2019-01-11 13:05:00,100001138_2019-02-11 14:09:00"


    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val nowTime = LocalDateTime.now()

    val ques_wValues = value1.split(",").map(x => {
      val kv = x.split("_")
      (kv(0), kv(1))
    })

    val ques_times = value2.split(",").map(x => {
      val kv = x.split("_")
      val time = LocalDateTime.parse(kv(1), formatter)
      (kv(0), ChronoUnit.MINUTES.between(time, nowTime))
    })

    val first = ques_wValues.map(_._1).toSet &~ ques_times.map(_._1).toSet
    if (first.isEmpty) {
      // 推20分钟以外的
      println(ques_times.filter(_._2 > 20).map(x => {
        (x._1, x._2)
      }).toList)
    } else {
      // 推没有做过的题
      println(ques_wValues.filter(x => {
        first.contains(x._1)
      }).toList)
    }
  }

}
