package com.test.demand.temp

/**
  * 全局枚举
  * 各业务枚举类继承此类
  * @author XiaShuai on 2020/5/12.
  */
abstract class GlobalEnum() extends Enumeration {

  class GlobalValue(i: Int, hiveSourceInfo: HiveSourceInfo, elasticSinkInfo: ElasticSinkInfo) extends Val(i) with HiveElasticConstruct {
    override def id: Int = i

    def hiveInfo: HiveSourceInfo = hiveSourceInfo

    def elasticInfo: ElasticSinkInfo = elasticSinkInfo
  }

  protected final def Value(i: Int, hiveSourceInfo: HiveSourceInfo, elasticSinkInfo: ElasticSinkInfo): GlobalValue = new GlobalValue(i, hiveSourceInfo, elasticSinkInfo)

  // 获取globalValues集合
  def globalValues: Seq[GlobalValue] = {
    val arr = super.values.toArray
    var builder = Seq.newBuilder[GlobalValue]
    for (value <- arr) {
      value match {
        case v: GlobalValue => builder.+=(v)
        case _ =>
      }
    }
    builder.result()
  }

  // 重写values,将globalValue从values中排除
  override def values: ValueSet = {
    val arr = super.values.toArray
    var builder = ValueSet.newBuilder
    for (value <- arr) {
      if (!value.isInstanceOf[GlobalValue]) builder.+=(value)
    }
    builder.result()
  }
}

case class HiveSourceInfo(hiveTable: String, hiveTypes: String, hiveFields: Array[String], hiveWhereStr: String)

case class ElasticSinkInfo(esIndex: String, esDeleteConfig: Map[String, String], esMapConf: Map[String, String])