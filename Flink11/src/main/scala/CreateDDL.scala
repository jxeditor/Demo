/**
  * @Author: xs
  * @Date: 2020-03-19 16:39
  * @Description:
  */
object CreateDDL {
  // 数据库表
  def createMysqlTable(): String = {
    "create table test (" +
      "`uid` INTEGER," +
      "`u_score` Timestamp(3)" + // MySQL中使用DateTime,FlinkSQL中可以使用timestamp(3)来接收
      ") with (" +
      " 'connector.type' = 'jdbc', " +
      " 'connector.url' = 'jdbc:mysql://localhost:3306/world', " +
      " 'connector.table' = 'test', " +
      " 'connector.driver' = 'com.mysql.jdbc.Driver', " +
      " 'connector.username' = 'root', " +
      " 'connector.password' = '123456'" +
      ")"
  }

  // Kafka表
  def createKafkaTable(): String = {
    // json-schema与derive-schema只能选一个使用
    "CREATE TABLE test (" +
      "before ROW<`ref` BIGINT,user_id STRING,reply_attach STRING>," +
      "after ROW<`ref` BIGINT>" +
      //      "`business` VARCHAR, " +
      //      "`database` VARCHAR, " +
      //      "`es` VARCHAR, " +
      //      "`sql` VARCHAR, " +  // 关键词不识别
      //      "`table` VARCHAR, " +  // 关键词不识别
      //      "`ts` BIGINT, " +
      //      "`rowtime` as TO_TIMESTAMP(FROM_UNIXTIME(ts / 1000,'yyyy-MM-dd HH:mm:ss')), " + // EventTime
      //      "`proctime` as PROCTIME(), " + // processTime
      //      "WATERMARK FOR rowtime as rowtime - INTERVAL '5' MINUTE" + // 设置水印
      ") WITH (" +
      " 'connector.type' = 'kafka', " +
      " 'connector.version' = 'universal', " +
      " 'connector.topic' = 'test', " +
      " 'update-mode' = 'append', " +
      " 'connector.properties.zookeeper.connect' = 'skuldcdhtest1.ktcs:2181', " +
      " 'connector.properties.bootstrap.servers' = 'skuldcdhtest1.ktcs:9092', " +
      " 'connector.properties.group.id' = 'kafkasql', " +
      //      " 'connector.specific-offsets' = 'partition:0,offset:42;partition:1,offset:300',"  // 从指定分区的offset开始消费
      //      " 'connector.startup-mode' = 'earliest-offset', " +
      " 'connector.startup-mode' = 'latest-offset', " +
      " 'format.type' = 'json', " +
      " 'format.derive-schema' = 'true'" +
      //      " 'format.json-schema' = " +
      //      "     '{" +
      //      "       \"type\":\"object\"," +
      //      "       \"properties\": { " +
      //      "         \"business\": {" +
      //      "             \"type\":\"string\"" +
      //      "           }," +
      //      "         \"ts\": {" +
      //      "             \"type\":\"number\" " +
      //      //      "             \"format\":\"date-time\"" +
      //      "           }" +
      //      "       }" +
      //      "     }'" +
      ")"
  }

  // HBase表
  def createHbaseTable(): String = {
    "create table test (" +
      "`name` string," +
      "`info` ROW<name varchar, age varchar>" +
      ") with (" +
      " 'connector.type' = 'hbase', " +
      " 'connector.version' = '1.4.3', " +
      " 'connector.table-name' = 'user', " +
      " 'connector.zookeeper.quorum' = 'cdh04:2181', " +
      " 'connector.zookeeper.znode.parent' = '/hbase', " +
      " 'connector.write.buffer-flush.max-size' = '1mb', " +
      " 'connector.write.buffer-flush.max-rows' = '1', " +
      " 'connector.write.buffer-flush.interval' = '2s' " +
      ")"
  }

}
