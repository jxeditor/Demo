package com.test.flink.stream.es.param

trait EnvironmentalKey {
  val CLUSTER_NAME = "es_cluster"
  val MAX_ACTION = "1000"
  val ES_NAME = "hadoop01"
  val ES_PORT = 9200
  val ES_INDEX = "test_index"
  val ES_TYPE = "test_type"

  val KAFKA_ZOOKEEPER = "hadoop01:2181"
  val BROKER = "hadoop03:9092"
  val GROUP_ID = "test"
  val TOPIC = "test"
  val HBASE_ZOOKEEPER = "hadoop01,hadoop02,hadoop03"
  val ZOOKEEPER_PORT = "2181"
  val TABLE_NAME = "test.demo_"
  val TABLE_CF = "info"
}