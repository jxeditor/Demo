package com.test.spark.batch.wc

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
 * @Author: xs
 * @Date: 2019-12-06 14:37
 * @Description:
 */
object WordCount {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")

    val spark = SparkSession.builder()
      .appName("WordCount")
      .master("local[*]")
      .getOrCreate()


    val lines = spark.read.textFile("D:\\工作\\IdeaProjects\\Demo\\Spark\\src\\main\\resources\\wc")
    import spark.implicits._
    val rdd = lines.flatMap(_.split(" ")).map((_, 1)).rdd.reduceByKey(_ + _)
    println(rdd.collect().toList)

    spark.stop()

    // val words: Dataset[String] = lines.flatMap(_.split(" "))
    // words.createTempView("t_wc")
    // val dataFrame: DataFrame = spark.sql("select value, count(*) counts from t_wc group by value order by value desc")
  }
}
