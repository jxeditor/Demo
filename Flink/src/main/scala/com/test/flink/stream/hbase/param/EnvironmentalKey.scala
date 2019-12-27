package com.test.flink.stream.hbase.param

trait EnvironmentalKey {
  val HBASE_ZOOKEEPER = "hadoop01,hadoop02,hadoop03"
  val ZOOKEEPER_PORT = "2181"
  val TABLE_NAME = "test.demo_"
  val TABLE_CF = "info"
  val KAFKA_ZOOKEEPER = "hadoop01:2181"
  val BROKER = "hadoop03:9092"
  val GROUP_ID = "test"
  val TOPIC = "test,test1"
}