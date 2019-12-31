package com.test.flink;

import org.apache.flink.table.descriptors.Schema;

import java.util.Map;

/**
 * @Author: xs
 * @Date: 2019-12-31 10:26
 * @Description:
 */
public class DemoSchema extends Schema {
    public Schema field(Map<String, String> map) {
        for (String fieldName :
                map.keySet()) {
            field(fieldName, map.get(fieldName));
        }
        return this;
    }
}
