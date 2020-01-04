package com.test.flink.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.table.functions.AsyncTableFunction;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.types.Row;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @Author: xs
 * @Date: 2020-01-03 17:21
 * @Description:
 */
public class RedisAsyncLookupFunction extends AsyncTableFunction<Row> {

    private final String[] fieldNames;
    private final TypeInformation[] fieldTypes;

    private transient RedisAsyncCommands<String, String> async;

    public RedisAsyncLookupFunction(String[] fieldNames, TypeInformation[] fieldTypes) {
        this.fieldNames = fieldNames;
        this.fieldTypes = fieldTypes;
    }

    @Override
    public void open(FunctionContext context) {
        //配置redis异步连接
        RedisClient redisClient = RedisClient.create("redis://hadoop02:6379/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        async = connection.async();
    }

    //每一条流数据都会调用此方法进行join
    public void eval(CompletableFuture<Collection<Row>> future, Object... paramas) {
        //表名、主键名、主键值、列名
        String[] info = {"userInfo", "userId", paramas[0].toString(), "userName"};
        String key = String.join(":", info);
        RedisFuture<String> redisFuture = async.get(key);

        redisFuture.thenAccept(new Consumer<String>() {
            @Override
            public void accept(String value) {
                future.complete(Collections.singletonList(Row.of(key, value, "aaa")));
                //todo
                // BinaryRow row = new BinaryRow(2);
            }
        });
    }

    @Override
    public TypeInformation<Row> getResultType() {
        return new RowTypeInfo(fieldTypes, fieldNames);
    }

    public static final class Builder {
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

        public RedisAsyncLookupFunction build() {
            return new RedisAsyncLookupFunction(fieldNames, fieldTypes);
        }
    }
}
