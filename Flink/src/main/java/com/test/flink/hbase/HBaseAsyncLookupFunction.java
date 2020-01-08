package com.test.flink.hbase;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.table.functions.AsyncTableFunction;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.types.Row;
import org.hbase.async.GetRequest;
import org.hbase.async.HBaseClient;
import org.hbase.async.KeyValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @Author: xs
 * @Date: 2020-01-03 17:21
 * @Description:
 */
public class HBaseAsyncLookupFunction extends AsyncTableFunction<Row> {

    private final String tableName;
    private final String[] fieldNames;
    private final TypeInformation[] fieldTypes;

    private transient HBaseClient hBaseClient;

    public HBaseAsyncLookupFunction(String tableName, String[] fieldNames, TypeInformation[] fieldTypes) {
        this.tableName = tableName;
        this.fieldNames = fieldNames;
        this.fieldTypes = fieldTypes;
    }

    @Override
    public void open(FunctionContext context) {
        hBaseClient = new HBaseClient("hadoop01,hadoop02,hadoop03");
    }

    //每一条流数据都会调用此方法进行join
    public void eval(CompletableFuture<Collection<Row>> future, Object... paramas) {
        //表名、主键名、主键值、列名
        String[] info = {"userInfo", "userId", paramas[0].toString(), "userName"};
        String key = String.join(":", info);
        GetRequest get = new GetRequest(tableName, key);
        Deferred<ArrayList<KeyValue>> arrayListDeferred = hBaseClient.get(get);
        arrayListDeferred.addCallbacks(new Callback<String, ArrayList<KeyValue>>() {
            @Override
            public String call(ArrayList<KeyValue> keyValues) throws Exception {
                String value;
                if (keyValues.size() == 0) {
                    value = null;
                } else {
                    StringBuilder valueBuilder = new StringBuilder();
                    for (KeyValue keyValue : keyValues) {
                        valueBuilder.append(new String(keyValue.value()));
                    }
                    value = valueBuilder.toString();
                }
                future.complete(Collections.singletonList(Row.of(key, value, "aaa")));
                return "";
            }
        }, new Callback<String, Exception>() {
            @Override
            public String call(Exception e) throws Exception {
                return "";
            }
        });
    }

    @Override
    public TypeInformation<Row> getResultType() {
        return new RowTypeInfo(fieldTypes, fieldNames);
    }

    public static final class Builder {
        private String tableName;
        private String[] fieldNames;
        private TypeInformation[] fieldTypes;

        private Builder() {
        }

        public static Builder getBuilder() {
            return new Builder();
        }

        public Builder withFieldNames(String[] fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public Builder withFieldTypes(TypeInformation[] fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }

        public Builder withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public HBaseAsyncLookupFunction build() {
            return new HBaseAsyncLookupFunction(tableName, fieldNames, fieldTypes);
        }
    }
}
