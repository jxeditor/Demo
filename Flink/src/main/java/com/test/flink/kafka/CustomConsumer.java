package com.test.flink.kafka;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author XiaShuai on 2020/4/16.
 */
public class CustomConsumer<T> extends FlinkKafkaConsumer011<T> {
    public CustomConsumer(String topic, DeserializationSchema<T> valueDeserializer, Properties props) {
        super(topic, valueDeserializer, props);
    }

    public CustomConsumer(String topic, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(topic, deserializer, props);
    }

    public CustomConsumer(List<String> topics, DeserializationSchema<T> deserializer, Properties props) {
        super(topics, deserializer, props);
    }

    public CustomConsumer(List<String> topics, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(topics, deserializer, props);
    }

    public CustomConsumer(Pattern subscriptionPattern, DeserializationSchema<T> valueDeserializer, Properties props) {
        super(subscriptionPattern, valueDeserializer, props);
    }

    public CustomConsumer(Pattern subscriptionPattern, KafkaDeserializationSchema<T> deserializer, Properties props) {
        super(subscriptionPattern, deserializer, props);
    }

    @Override
    public void run(SourceContext<T> sourceContext) throws Exception {
        super.run(sourceContext);
    }
}
