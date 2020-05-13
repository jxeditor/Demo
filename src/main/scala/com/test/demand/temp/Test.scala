package com.test.demand.temp

/**
  * @author XiaShuai on 2020/5/12.
  */
object Test {
  def main(args: Array[String]): Unit = {
    val adsGlobalValues = AdsEnum.globalValues.toArray
    val odsGlobalValues = OdsEnum.globalValues.toArray

    val adsPrimaryValues = AdsEnum.values.toArray

    adsGlobalValues.foreach((f: HiveElasticConstruct) => {
      println(f.hiveInfo.hiveTable)
    })

    adsPrimaryValues.foreach(x => {
      println(x.id)
    })

    println(adsGlobalValues.length)
    println(odsGlobalValues.length)
  }
}
