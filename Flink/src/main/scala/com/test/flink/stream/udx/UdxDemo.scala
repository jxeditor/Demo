package com.test.flink.stream.udx

import java.util.TimeZone

import com.test.flink.stream.udx.udaf.CollectListUDAF
import com.test.flink.stream.udx.udf.UpperUDF
import com.test.flink.stream.udx.udtf.{TransformC2RUDTF, TransformR2CUDTF}
import org.apache.flink.api.java.io.jdbc.{JDBCOptions, JDBCTableSource}
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.scala.{StreamTableEnvironment, _}
import org.apache.flink.table.api.{DataTypes, EnvironmentSettings, TableEnvironment, TableSchema}
import org.apache.flink.types.Row

/**
 * @Author: xs
 * @Date: 2020-02-17 20:50
 * @Description:
 */
object UdxDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(env, bsSettings)
    val byteSourceTable = env.fromElements(
      WC("{\"name\":\"Alice\",\"age\":13,\"grade\":\"A\"}"),
      WC("{\"name\":\"Jack\",\"age\":14,\"grade\":\"B\"}")).toTable(tableEnv)

    tableEnv.registerTable("b", byteSourceTable)
    tableEnv.registerFunction("collect_list", new CollectListUDAF)
    tableEnv.registerFunction("UpperUDF", new UpperUDF)
    tableEnv.registerFunction("TransformR2CUDTF", new TransformR2CUDTF)
    tableEnv.registerFunction("TransformC2RUDTF", new TransformC2RUDTF)

    val res1 = tableEnv.sqlQuery("select  T.name as name, T.age as age, T.grade as grade from b as S LEFT JOIN LATERAL TABLE(TransformR2CUDTF(message)) as T(name, age, grade) ON TRUE")
    val res2 = tableEnv.sqlQuery(s"select name,concat_ws(',',name,cast(age as varchar),grade) as attrs from $res1")
    val res3 = tableEnv.sqlQuery(s"select S.name,T.attr as attr from $res2 as S LEFT JOIN LATERAL TABLE(TransformC2RUDTF(attrs)) as T(attr) ON TRUE")
    val res4 = tableEnv.sqlQuery(s"select name,LISTAGG(attr) as attrs from $res3 group by name")
    // 无窗口的情况下,groupby之后是一个Retract流
    res4.toRetractStream[Row].print()

    tableEnv.execute("test")
  }

  case class WC(message: String)

}
