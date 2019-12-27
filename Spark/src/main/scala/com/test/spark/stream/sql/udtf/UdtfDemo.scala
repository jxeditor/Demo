package com.test.spark.stream.sql.udtf

import java.util

import org.apache.hadoop.hive.ql.exec.{UDFArgumentException, UDFArgumentLengthException}
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF
import org.apache.hadoop.hive.serde2.objectinspector.{ObjectInspector, ObjectInspectorFactory}
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory
import org.json.JSONObject

/**
 * @Author: xs
 * @Date: 2019-12-18 15:57
 * @Description:
 */
class UdtfDemo extends GenericUDTF {
  // 这是处理数据的方法，入参数组里只有1行数据,即每次调用process方法只处理一行数据
  override def process(objects: Array[AnyRef]): Unit = {
    //将字符串切分成单个字符的数组
    val obj = new JSONObject(objects(0).toString)
    val tmp: Array[String] = new Array[String](1)
    tmp(0) = obj.getString("topic")
    tmp(1) = obj.getString("payload")
    tmp(2) = obj.getString("offset")

    forward(tmp)
  }

  override def close(): Unit = {

  }
}
