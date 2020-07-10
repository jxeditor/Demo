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
    System.setProperty("fs.defaultFS","hdfs://skuldcdhtest1.ktcs:8020")
    val bsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val bsSettings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build
    bsEnv.enableCheckpointing(10000)
    bsEnv.setParallelism(1)
    val tableEnv = StreamTableEnvironment.create(bsEnv, bsSettings)
    val kafkaTable = CreateDDL.createKafkaTable()
    tableEnv.executeSql(kafkaTable)

    //    val table = tableEnv.sqlQuery("select before.user_id,before.reply_attach,before.`ref` ref1, after.`ref` ref2 from test")
    val sql = "CREATE TABLE fs_table (" +
      "    user_uid  STRING," +
      "    `ref` BIGINT," +
      "    reply_attach STRING," +
      "  dt STRING," +
      "  h string" +
      ")  PARTITIONED BY (dt,h)  WITH (" +
      "  'connector'='filesystem'," +
      "  'path'='hdfs:///tmp/test'," +
      "  'sink.partition-commit.policy.kind' = 'success-file', " +
      "  'format'='orc'" +
      ")"
    tableEnv.executeSql(sql);

    tableEnv.executeSql(
      s"""
         |insert into fs_table
         |select before.user_id,before.`ref`,before.reply_attach,
         |DATE_FORMAT(LOCALTIMESTAMP, 'yyyy-MM-dd'),
         |DATE_FORMAT(LOCALTIMESTAMP, 'HH')
         |FROM test
         |""".stripMargin)

    //    bsEnv.execute("")
//    tableEnv.execute("")
  }
}
