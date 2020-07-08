package com.test.flink;

import org.apache.avro.JsonProperties;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableException;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.factories.TableFactory;

import java.util.*;

/**
 * @author XiaShuai on 2020/6/11.
 */
public class Demo {
    public static void main(String[] args) throws Exception {
        List<TableFactory> tableFactories = discoverFactories(Optional.empty());
        for (int i = 0; i < tableFactories.size(); i++) {
            System.out.println(tableFactories.get(i).toString());
        }
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettings settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);
        TupleTypeInfo tupleTypeInfo = new
                TupleTypeInfo(BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.STRING_TYPE_INFO);
        DataStream<GenericRecord> testDataStream = tEnv.toAppendStream(null, tupleTypeInfo);
        testDataStream.print().setParallelism(1);
        ArrayList<Schema.Field> fields = new
                ArrayList<org.apache.avro.Schema.Field>();
        fields.add(new org.apache.avro.Schema.Field("id",
                org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
                "id", JsonProperties.NULL_VALUE));
        fields.add(new org.apache.avro.Schema.Field("time",
                org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING),
                "time", JsonProperties.NULL_VALUE));
        org.apache.avro.Schema parquetSinkSchema =
                org.apache.avro.Schema.createRecord("pi", "flinkParquetSink",
                        "flink.parquet", true, fields);
        String fileSinkPath = "./xxx.text/rs6/";
        StreamingFileSink<GenericRecord> parquetSink = StreamingFileSink.
                forBulkFormat(new Path(fileSinkPath),
                        ParquetAvroWriters.forGenericRecord(parquetSinkSchema))
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .build();
        testDataStream.addSink(parquetSink).setParallelism(1);
        tEnv.execute("");
    }

    private static List<TableFactory> discoverFactories(Optional<ClassLoader> classLoader) {
        try {
            List<TableFactory> result = new LinkedList();
            ClassLoader cl = (ClassLoader) classLoader.orElse(Thread.currentThread().getContextClassLoader());
            ServiceLoader.load(TableFactory.class, cl).iterator().forEachRemaining(result::add);
            return result;
        } catch (ServiceConfigurationError var3) {
            throw new TableException("Could not load service provider for table factories.", var3);
        }
    }
}
