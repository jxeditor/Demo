package com.test.flink.stream.udx.udtf

import com.alibaba.fastjson.JSON
import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.table.functions.TableFunction
import org.apache.flink.types.Row


/**
 * @Author: xs
 * @Date: 2020-02-17 21:00
 * @Description: 行转多列
 */
class TransformC2RUDTF extends TableFunction[Row] {

  def eval(str: String) = {
    val arr = str.split(",")
    arr.map(x => {
      val row = new Row(1)
      row.setField(0, x)
      collect(row)
    })
  }

  override def getResultType: TypeInformation[Row] = {
    Types.ROW_NAMED(Array("attr"), Types.STRING)
  }
}
