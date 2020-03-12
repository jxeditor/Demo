package com.test.spark.stream.sql

import org.apache.spark
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

/**
 * @Author: xs
 * @Date: 2020-02-19 10:56
 * @Description:
 */
object LocalSqlDemo {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .master("local")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()
    val all = spark.read.json("D:\\工作\\IdeaProjects\\Demo\\Spark\\src\\main\\resources\\all.json").selectExpr("name", "be", "'all' as flag", "all_count as count")
    val tea = spark.read.json("D:\\工作\\IdeaProjects\\Demo\\Spark\\src\\main\\resources\\tea.json").selectExpr("name", "be", "'tea' as flag", "tea_count as count")
    val stu = spark.read.json("D:\\工作\\IdeaProjects\\Demo\\Spark\\src\\main\\resources\\stu.json").selectExpr("name", "be", "'stu' as flag", "stu_count as count")
    all.union(tea).union(stu).selectExpr(
      "name", "be",
      "if(flag = 'all', count, 0) as all_count",
      "if(flag = 'tea', count, 0) as tea_count",
      "if(flag = 'stu', count, 0) as stu_count"
    ).groupBy(
      "name", "be"
    ).agg(
      sum("all_count").as("all_count"),
      sum("tea_count").as("tea_count"),
      sum("stu_count").as("stu_count")
    ).selectExpr("name", "be").filter("all_count = 122").show()



    //    all.join(
    //      tea,Seq("name","be"),"left"
    //    ).join(
    //      stu,Seq("name","be"),"left"
    //    ).show()

    all.union(tea).union(stu).selectExpr(
      "ref",
      "IF(flag = 0,user_id,NULL) as user_id_0",
      "IF(flag = 0,paper_id,NULL) as paper_id_0",
      "IF(flag = 0,ques_id,NULL) as ques_id_0",
      "IF(flag = 0,score,NULL) as score_0",
      "IF(flag = 0,is_right,NULL) as is_right_0",
      "IF(flag = 0,answer,NULL) as answer_0",
      "IF(flag = 0,c_time,NULL) as c_time_0",
      "IF(flag = 0,is_marking,NULL) as is_marking_0",
      "IF(flag = 0,annexname,NULL) as annexname_0",
      "IF(flag = 0,task_id,NULL) as task_id_0",
      "IF(flag = 0,attach_type,NULL) as attach_type_0",
      "IF(flag = 0,mark_comment,NULL) as mark_comment_0",
      "IF(flag = 0,voice_time,NULL) as voice_time_0",
      "IF(flag = 0,insert_from,NULL) as insert_from_0",
      "IF(flag = 0,insert_time,NULL) as insert_time_0",
      "IF(flag = 1,user_id,NULL) as user_id_1",
      "IF(flag = 1,paper_id,NULL) as paper_id_1",
      "IF(flag = 1,ques_id,NULL) as ques_id_1",
      "IF(flag = 1,score,NULL) as score_1",
      "IF(flag = 1,is_right,NULL) as is_right_1",
      "IF(flag = 1,answer,NULL) as answer_1",
      "IF(flag = 1,c_time,NULL) as c_time_1",
      "IF(flag = 1,is_marking,NULL) as is_marking_1",
      "IF(flag = 1,annexname,NULL) as annexname_1",
      "IF(flag = 1,task_id,NULL) as task_id_1",
      "IF(flag = 1,attach_type,NULL) as attach_type_1",
      "IF(flag = 1,mark_comment,NULL) as mark_comment_1",
      "IF(flag = 1,voice_time,NULL) as voice_time_1",
      "IF(flag = 1,insert_from,NULL) as insert_from_1",
      "IF(flag = 1,insert_time,NULL) as insert_time_1"
    ).groupBy(
      "ref"
    ).agg(
      max("user_id_0").as("user_id_0"),
      max("paper_id_0").as("paper_id_0"),
      max("ques_id_0").as("ques_id_0"),
      max("score_0").as("score_0"),
      max("is_right_0").as("is_right_0"),
      max("answer_0").as("answer_0"),
      max("c_time_0").as("c_time_0"),
      max("is_marking_0").as("is_marking_0"),
      max("annexname_0").as("annexname_0"),
      max("task_id_0").as("task_id_0"),
      max("attach_type_0").as("attach_type_0"),
      max("mark_comment_0").as("mark_comment_0"),
      max("voice_time_0").as("voice_time_0"),
      max("insert_from_0").as("insert_from_0"),
      max("insert_time_0").as("insert_time_0"),
      max("user_id_1").as("user_id_1"),
      max("paper_id_1").as("paper_id_1"),
      max("ques_id_1").as("ques_id_1"),
      max("score_1").as("score_1"),
      max("is_right_1").as("is_right_1"),
      max("answer_1").as("answer_1"),
      max("c_time_1").as("c_time_1"),
      max("is_marking_1").as("is_marking_1"),
      max("annexname_1").as("annexname_1"),
      max("task_id_1").as("task_id_1"),
      max("attach_type_1").as("attach_type_1"),
      max("mark_comment_1").as("mark_comment_1"),
      max("voice_time_1").as("voice_time_1"),
      max("insert_from_1").as("insert_from_1"),
      max("insert_time_1").as("insert_time_1")
    ).selectExpr(
      "ref",
      "IF(user_id_0 is null,user_id_1,user_id_0)",
      "IF(paper_id_0 is null,paper_id_1,paper_id_0)",
      "IF(ques_id_0 is null,ques_id_1,ques_id_0)",
      "IF(score_0 is null,score_1,score_0)",
      "IF(is_right_0 is null,is_right_1,is_right_0)",
      "IF(answer_0 is null,answer_1,answer_0)",
      "IF(c_time_0 is null,c_time_1,c_time_0)",
      "IF(is_marking_0 is null,is_marking_1,is_marking_0)",
      "IF(annexname_0 is null,annexname_1,annexname_0)",
      "IF(task_id_0 is null,task_id_1,task_id_0)",
      "IF(attach_type_0 is null,attach_type_1,attach_type_0)",
      "IF(mark_comment_0 is null,mark_comment_1,mark_comment_0)",
      "IF(voice_time_0 is null,voice_time_1,voice_time_0)",
      "IF(insert_from_0 is null,insert_from_1,insert_from_0)",
      "IF(insert_time_0 is null,insert_time_1,insert_time_0)"
    ).show()
  }
}
