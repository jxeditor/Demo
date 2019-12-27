package com.test.spark.stream.hbase.util

import java.util.{HashMap, Properties}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{HBaseConfiguration, HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.mapred.JobConf

/**
 * @Author: xs
 * @Date: 2019-12-10 08:50
 * @Description:
 */
object HBaseUtil {
  var conn: Connection = null
  var tables: HashMap[String, Table] = new HashMap[String, Table]

  def initConn() {
    if (conn == null || conn.isClosed) {
      println("----  Init Conn  -----")
      val conf = getConf()
      conn = ConnectionFactory.createConnection(conf)
    }
  }

  def getConn() = {
    initConn
    conn
  }

  def getConf() = {
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER)
    conf.set("hbase.zookeeper.property.clientPort", "2181")
    conf
  }

  def getTable(tablename: String) = {
    if (!getConn().getAdmin.tableExists(TableName.valueOf(tablename))) {
      conn.getAdmin.createTable(
        new HTableDescriptor(
          TableName.valueOf(tablename)
        ).addFamily(
          new HColumnDescriptor("info")
        ))
    }
    tables.getOrDefault(tablename, {
      initConn
      conn.getTable(TableName.valueOf(tablename))
    })
  }

  def put(tableName: String, p: Put) {
    getTable(tableName)
      .put(p)
  }

  def get(tableName: String, get: Get, cf: String, column: String) = {
    val r = getTable(tableName)
      .get(get)
    if (r != null && !r.isEmpty()) {
      new String(r.getValue(cf.getBytes, column.getBytes))
    } else null
  }

  //  接受配置文件
  /**
   * 用于直接建立HBase连接
   *
   * @param properties
   * @return
   */
  def getConf(properties: Properties) = {
    val conf = HBaseConfiguration.create()
    conf.set("hbase.zookeeper.quorum", properties.getProperty("hbase.zookeeper.quorum"))
    conf.set("hbase.zookeeper.property.clientPort", properties.getProperty("hbase.zookeeper.property.clientPort"))
    conf.set("hbase.master", properties.getProperty("hbase.master"))
    conf
  }

  /**
   * 获取连接
   *
   * @param conf
   * @return
   */
  def getConn(conf: Configuration) = {
    if (conn == null || conn.isClosed) {
      conn = ConnectionFactory.createConnection(conf)
    }
    conn
  }

  /**
   * 获取表,没有表则创建
   *
   * @param conn
   * @param tableName
   * @return
   */
  def getTable(conn: Connection, tableName: String) = {
    createTable(conn, tableName)
    conn.getTable(TableName.valueOf(tableName))
  }

  /**
   * 创建表
   *
   * @param conn
   * @param tableName
   */
  def createTable(conn: Connection, tableName: String) = {
    if (!conn.getAdmin.tableExists(TableName.valueOf(tableName))) {
      val tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName))
      tableDescriptor.addFamily(new HColumnDescriptor("info".getBytes()))
      conn.getAdmin.createTable(tableDescriptor)
    }
  }

  /**
   * 提交数据
   *
   * @param conn
   * @param tableName
   * @param data
   */
  def putData(conn: Connection, tableName: String, data: Put) = {
    getTable(conn, tableName).put(data)
  }

  /**
   * 对表直接进行批量写入时使用
   *
   * @param conf
   * @param tableName
   * @return
   */
  def getNewJobConf(conf: Configuration, tableName: String) = {
    conf.set("hbase.defaults.for.version.skip", "true")
    conf.set(org.apache.hadoop.hbase.mapreduce.TableOutputFormat.OUTPUT_TABLE, tableName)
    conf.setClass("mapreduce.job.outputformat.class", classOf[org.apache.hadoop.hbase.mapreduce.TableOutputFormat[String]], classOf[org.apache.hadoop.mapreduce.OutputFormat[String, Mutation]])
    new JobConf(conf)
  }
}
