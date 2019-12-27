package com.test.demand

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

/**
 * @Author: xs
 * @Date: 2019-12-23 16:45
 * @Description:
 */
object DateDemo {
  def main(args: Array[String]): Unit = {
    println(LocalDateTime.ofInstant(
      Instant.ofEpochMilli(1576631741795L), ZoneOffset.ofHours(8)
    ).toLocalDate.toString)
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    println(LocalDateTime.ofInstant(
      Instant.ofEpochMilli(1576631741795L), ZoneOffset.ofHours(8)).format(df))
  }
}
