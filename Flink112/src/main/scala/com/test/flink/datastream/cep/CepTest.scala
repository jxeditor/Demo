package com.test.flink.datastream.cep

import java.sql.Timestamp
import java.util
import java.util.UUID

import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.cep.PatternSelectFunction
import org.apache.flink.cep.pattern.conditions.IterativeCondition
import org.apache.flink.cep.scala.CEP
import org.apache.flink.cep.scala.pattern.Pattern
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.table.api._

/**
 *
 * @author tianchen by 2020/11/11
 */
object CepTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val ds = env.addSource(new SourceFunction[(String,Long,Int,Int)] {
      val status: Array[Int] = Array(200, 404, 500, 501, 301)

      override def run(ctx: SourceFunction.SourceContext[(String,Long,Int,Int)]): Unit = {
        while ( {
          true
        }) {
          Thread.sleep((Math.random * 100).toInt)
          // traceid,timestamp,status,response time
          val log = (UUID.randomUUID.toString, System.currentTimeMillis, status((Math.random * 4).toInt), (Math.random * 100).toInt)
          ctx.collect(log)
        }
      }

      override def cancel(): Unit = {

      }
    })

    val tenv = StreamTableEnvironment.create(env)
    tenv.createTemporaryView("log", ds,'traceid,'timestamp,'status,'restime,'proctime.proctime())

    val sql =
      s"""
         |select pv,
         |  errorcount,
         |  round(CAST(errorcount AS DOUBLE)/pv,2) as errorRate,
         |  (starttime + interval '8' hour) as stime,
         |  (endtime + interval '8' hour) as etime
         |from (
         |  select count(*) as pv,
         |    sum(case when status = 200 then 0 else 1 end) as errorcount,
         |    TUMBLE_START(proctime,INTERVAL '1' SECOND)  as starttime,
         |    TUMBLE_END(proctime,INTERVAL '1' SECOND)  as endtime
         |  from log
         |  group by TUMBLE(proctime,INTERVAL '1' SECOND))
         |""".stripMargin

    val table = tenv.sqlQuery(sql)
    val ds1 = tenv.toAppendStream[Result](table)

    ds1.print

    val pattern = Pattern.begin[Result]("alert").where(new IterativeCondition[Result] {
      override def filter(t: Result, context: IterativeCondition.Context[Result]): Boolean = {
        t.errorRate > 0.7D

      }
    }).times(3).consecutive.followedBy("recovery").where(new IterativeCondition[Result] {
      override def filter(t: Result, context: IterativeCondition.Context[Result]): Boolean = {
        t.errorRate <= 0.7D
      }
    }).optional

    val alertStream = CEP.pattern(ds1, pattern).select(new PatternSelectFunction[Result,util.Map[String,util.List[Result]]] {
      override def select(map: util.Map[String, util.List[Result]]): util.Map[String, util.List[Result]] = {
        val alertList = map.get("alert")
        val recoveryList = map.get("recovery")
        if (recoveryList != null) {
          println("接受到了报警恢复的信息，报警信息如下：")
          println(alertList)
          println("  对应的恢复信息：")
          println(recoveryList)
        }
        else {
          println("收到了报警信息 ")
          println(alertList)
        }
        map
      }
    })

    env.execute("Flink CEP web alert")
  }

  case class Result(pv:Long,errorcount:Int,errorRate:Double,stime:Timestamp,etime:Timestamp)
}
