package com.test.demand.temp

/**
  * @author XiaShuai on 2020/5/12.
  */
case object AdsEnum extends GlobalEnum {
  // GlobalValue
  val a = Value(1, HiveSourceInfo("ads_table_a", "partition_a", Array("field1", "field2"), "where 1=0"), ElasticSinkInfo("index_a", Map(), Map()))
  val b = Value(2, HiveSourceInfo("ads_table_b", "partition_b", Array("field1", "field2"), "where 1=0"), ElasticSinkInfo("index_b", Map(), Map()))

  // 原生Value
  val c = Value("原生")
}