package com.test.spark.batch.es

import org.apache.spark.sql.SparkSession
import org.elasticsearch.spark._
import org.elasticsearch.spark.sql.EsSparkSQL

/**
 * @Author: xs
 * @Date: 2019-12-06 15:23
 * @Description:
 */
object WriteToEsDemo {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "D:\\DevEnv\\Hadoop\\2.6.0")
    val spark: SparkSession = SparkSession.builder().appName("WriteToEsDemo")
      .master("local[*]")
      .config("es.index.auto.create", "true")
      .config("es.nodes", "ip1:9200,ip2:9200,ip2:9200")
      .getOrCreate()

    // EsSparkSQL.esDF(spark,"/test")

    val lines = spark.read.textFile("D:\\工作\\IdeaProjects\\Demo\\Spark\\src\\main\\resources\\wc")
    import spark.implicits._
    val rdd = lines.flatMap(_.split(" ")).map((_, 1)).rdd.reduceByKey(_ + _)

    rdd.saveToEs("user_index",Map("es.mapping.id" -> "id"))
    println(rdd.collect().toList)
  }

  case class User(id: String, user_name: String, user_age: String)

}
