package com.test.flink.stream.es.entry

import com.test.flink.stream.es.develop.{ES_INDEX, ES_TYPE}
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.common.xcontent.json.JsonXContent
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.script.Script

class TestElasticsearchSinkFunction extends ElasticsearchSinkFunction[(String, Int)] {
  override def process(data: (String, Int), runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
    val id = data._1
    val content = JsonXContent.contentBuilder().startObject()
      .field("id", id)
      .field("word", data._1)
      .field("count", data._2)
      .endObject()
    //    val indexRequest = new IndexRequest().index(
    //      ES_INDEX
    //    ).`type`(
    //      ES_TYPE
    //    ).id(id).source(content)
    //    requestIndexer.add(indexRequest)
    //    val deleteRequest = new DeleteRequest().index(
    //      ES_INDEX
    //    ).`type`(
    //      ES_TYPE
    //    ).id(id)
    //
    //    requestIndexer.add(deleteRequest)


    val builder = QueryBuilders.boolQuery()
      .must(QueryBuilders.rangeQuery("e_time").gte("2020-04-09 00:00:00"))
      .must(QueryBuilders.boolQuery()
        .should(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("task_id", "123456")))
        .should(QueryBuilders.boolQuery()
          .must(QueryBuilders.termQuery("task_id", "12345678"))
          .must(QueryBuilders.termQuery("class_id", "110")))
      )

    println(id)

    val updateRequest1 = new UpdateRequest().index(
      ES_INDEX
    ).`type`(
      ES_TYPE
    ).id(id)
      .docAsUpsert(true).doc(content)

    val updateRequest = new UpdateRequest().index(
      ES_INDEX
    ).`type`(
      ES_TYPE
    ).id(id)
      .script(new Script("ctx._source.remove(\"word\")")).scriptedUpsert(true)
    //.docAsUpsert(true).doc(content)
    // doc对存在的数据进行修改,upsert对不存在的数据进行添加
    requestIndexer.add(updateRequest1)
    requestIndexer.add(updateRequest)
  }
}
