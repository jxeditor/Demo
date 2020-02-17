package com.test.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: xs
 * @Date: 2020-01-17 11:06
 * @Description:
 */
public class PushgatewayClient {
    public static void main(String[] args) {
        String host = "cdh04";
        int port = 9091;
        String jobName = "test";
        Map<String, String> groupingKey =new HashMap<>();
        groupingKey.put("labelKey", "s");

        PushGateway pushGateway = new PushGateway(host + ':' + port);

        try {
            pushGateway.push(CollectorRegistry.defaultRegistry, jobName, groupingKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
