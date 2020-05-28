package com.test.demand.temp

/**
  * @author XiaShuai on 2020/5/12.
  */
object Test {

  case class GenericElasticSinkInfo(hiveType: String, hiveWhere: (String, String, Boolean) => String)

  def main(args: Array[String]): Unit = {

    val info = GenericElasticSinkInfo(hiveType = "login", (app, dt, selectAll) => {
//      s"app='$app' and dt='$dt' and event='$hiveType'"
    })

    info.hiveWhere("yys", "2020-05-23", true)

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
