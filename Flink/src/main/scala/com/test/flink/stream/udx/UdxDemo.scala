package com.test.flink.stream.udx

import java.util.TimeZone
import com.test.flink.stream.udx.udaf.CollectListUDAF
import com.test.flink.stream.udx.udtf.TransformUDTF
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
    val byteSourceTable = env.fromElements(WC("{\"name\":\"Alice\",\"age\":13,\"grade\":\"A\"}")).toTable(tableEnv)

    tableEnv.registerTable("b", byteSourceTable)
    tableEnv.registerFunction("collect_list", new CollectListUDAF)
    tableEnv.registerFunction("TransformUDTF", new TransformUDTF)

    val res = tableEnv.sqlQuery("select * from b")
    res.toAppendStream[Row].print()

    val res1 = tableEnv.sqlQuery("select  T.name, T.age, T.grade\n" + "from b as S\n" + "LEFT JOIN LATERAL TABLE(TransformUDTF(message)) as T(name, age, grade) ON TRUE")
    res1.toAppendStream[Row].print()

    tableEnv.execute("test")
  }

  case class WC(message: String)

}
