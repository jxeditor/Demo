import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.types.Row
import org.apache.flink.api.scala._

/**
  * @Author: xs
  * @Date: 2020-03-10 09:57
  * @Description:
  */
object SQLFormatDemo {
  def main(args: Array[String]): Unit = {
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)

    //    val mysqlTable = CreateDDL.createMysqlTable()
    //    tableEnv.sqlUpdate(mysqlTable)

    val kafkaTable = CreateDDL.createKafkaTable()
    tableEnv.sqlUpdate(kafkaTable)

    //    tableEnv.toRetractStream[Row](tableEnv.sqlQuery("select uid,listagg(cast(u_score as string)) from test group by uid")).print()
    val table = tableEnv.sqlQuery("select before.user_id,before.reply_attach,before.`ref` ref1, after.`ref` ref2 from test")

    tableEnv.toAppendStream[Row](table).print()

    println("Explain: " + table.explain())

    bsEnv.execute("")
    tableEnv.execute("")
  }
}
