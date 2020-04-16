package com.test.flink.stream.hive

import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.json.JSONObject

class JsonDeserializationSchema extends KafkaDeserializationSchema[String] {

  override def isEndOfStream(nextElement: String) = false

  override def deserialize(record: ConsumerRecord[Array[Byte], Array[Byte]]): String = {
    val json = new JSONObject()

    json.put("topic", record.topic)
    json.put("partition", record.partition)
    json.put("offset", record.offset())
    json.put("timestamp", record.timestamp())
    json.put("key", if (record.key() == null) null else new String(record.key()))
    json.put("value", if (record.value() == null) null else new String(record.value()))
    json.toString
  }

  override def getProducedType: TypeInformation[String] = BasicTypeInfo.STRING_TYPE_INFO
}
