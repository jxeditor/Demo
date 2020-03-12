package com.test.demand

import java.io.FileInputStream
import java.util.Properties

/**
 * @Author: xs
 * @Date: 2020-02-07 16:11
 * @Description:
 */
object PropsDemo {
  def main(args: Array[String]): Unit = {
    val config = new FileInputStream("D:\\工作\\IdeaProjects\\Demo\\src\\main\\resources\\prop.properties")
    val properties = new Properties()
    properties.load(config)
    config.close()
    println(properties.getProperty("name"))
    var flag = properties.getProperty("flag").toBoolean
    //    while (flag) {
    //      println("1")
    //      val stateFile = new FileInputStream("D:\\工作\\IdeaProjects\\Demo\\src\\main\\resources\\prop.properties")
    //      properties.load(stateFile)
    //      stateFile.close()
    //      flag = properties.getProperty("flag").toBoolean
    //    }


    println(10 / 1)
    println(10 / 0)
    println(10 / 2)


    print("2222")
    //    val properties1 = properties.setProperty("test","1").asInstanceOf[Properties]
    //    val properties2 = properties.setProperty("test","2")
    //    val properties3 = properties.setProperty("test","3")
    //    val properties4 = properties.setProperty("test","4")

  }
}
