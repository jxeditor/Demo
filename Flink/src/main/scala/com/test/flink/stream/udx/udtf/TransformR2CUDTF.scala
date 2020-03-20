package com.test.flink.stream.udx.udtf

import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import org.apache.commons.compress.utils.Lists
import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.table.functions.TableFunction
import org.apache.flink.types.Row


/**
 * @Author: xs
 * @Date: 2020-02-17 21:00
 * @Description: 行转多列
 */
class TransformR2CUDTF extends TableFunction[Row] {

  def eval(str: String) = {
    val obj = JSON.parseObject(str)
    val row = new Row(3)
    row.setField(0,obj.getString("name"))
    row.setField(1,obj.getInteger("age"))
    row.setField(2,obj.getString("grade"))
    collect(row)
  }

  override def getResultType: TypeInformation[Row] = {
    Types.ROW_NAMED(Array("name", "age", "grade"), Types.STRING, Types.INT, Types.STRING)
  }
}
