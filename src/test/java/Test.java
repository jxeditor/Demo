import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author XiaShuai on 2020/5/9.
 */
public class Test {
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
