package com.test.demand.temp

/**
  * @author XiaShuai on 2020/5/12.
  */
trait HiveElasticConstruct {
  def hiveInfo: HiveSourceInfo

  def elasticInfo: ElasticSinkInfo
}
