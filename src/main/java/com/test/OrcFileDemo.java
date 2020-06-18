package com.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcFile;
import org.apache.orc.TypeDescription;
import org.apache.orc.Writer;

import java.io.IOException;
import java.util.UUID;

/**
 * @author XiaShuai on 2020/6/16.
 */
public class OrcFileDemo {
    public static void main(String[] args) throws IOException {
        Configuration conf = new Configuration();
        TypeDescription schema = TypeDescription.createStruct()
                .addField("platform", TypeDescription.createString())
                .addField("event", TypeDescription.createString())
                .addField("dt", TypeDescription.createString());

        Writer writer = OrcFile.createWriter(new Path("/tmp/my-file1.orc"),
                OrcFile.writerOptions(conf)
                        .setSchema(schema)
                        .version(OrcFile.Version.V_0_12)
                        .compress(CompressionKind.ZLIB)
        );

        VectorizedRowBatch batch = schema.createRowBatch();
        BytesColumnVector platform = (BytesColumnVector) batch.cols[0];
        BytesColumnVector event = (BytesColumnVector) batch.cols[1];
        BytesColumnVector dt = (BytesColumnVector) batch.cols[2];

        for (int r = 0; r < 1; ++r) {
            int row = batch.size++;
            platform.setVal(row, "test".getBytes());
            event.setVal(row, "test".getBytes());
            dt.setVal(row, "test".getBytes());
            if (batch.size == batch.getMaxSize()) {
                writer.addRowBatch(batch);
                batch.reset();
            }
        }
        if (batch.size != 0) {
            writer.addRowBatch(batch);
            batch.reset();
        }
        writer.close();
    }
}
