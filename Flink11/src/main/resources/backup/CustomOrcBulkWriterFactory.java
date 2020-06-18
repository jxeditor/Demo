//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.serialization.BulkWriter;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.orc.vector.Vectorizer;
import org.apache.flink.util.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.orc.OrcFile;
import org.apache.orc.impl.WriterImpl;
import org.apache.orc.OrcFile.*;

@PublicEvolving
public class CustomOrcBulkWriterFactory<T> implements BulkWriter.Factory<T> {
    private static final Path FIXED_PATH = new Path(".");
    private final Vectorizer<T> vectorizer;
    private final Properties writerProperties;
    private final Map<String, String> confMap;
    private WriterOptions writerOptions;

    public CustomOrcBulkWriterFactory(Vectorizer<T> vectorizer) {
        this(vectorizer, new Configuration());
    }

    public CustomOrcBulkWriterFactory(Vectorizer<T> vectorizer, Configuration configuration) {
        this(vectorizer, (Properties) null, configuration);
    }

    public CustomOrcBulkWriterFactory(Vectorizer<T> vectorizer, Properties writerProperties, Configuration configuration) {
        this.vectorizer = (Vectorizer) Preconditions.checkNotNull(vectorizer);
        this.writerProperties = writerProperties;
        this.confMap = new HashMap();
        Iterator var4 = configuration.iterator();

        while (var4.hasNext()) {
            Entry<String, String> entry = (Entry) var4.next();
            this.confMap.put(entry.getKey(), entry.getValue());
        }

    }

    public BulkWriter<T> create(FSDataOutputStream out) throws IOException {
        WriterOptions opts = this.getWriterOptions();
        opts.physicalWriter(new CustomPhysicalWriterImpl(out, opts));
        return new CustomOrcBulkWriter(this.vectorizer, new WriterImpl((FileSystem) null, FIXED_PATH, opts));
    }

    private WriterOptions getWriterOptions() {
        if (null == this.writerOptions) {
            Configuration conf = new Configuration();
            Iterator var2 = this.confMap.entrySet().iterator();

            while (var2.hasNext()) {
                Entry<String, String> entry = (Entry) var2.next();
                conf.set((String) entry.getKey(), (String) entry.getValue());
            }

            this.writerOptions = OrcFile.writerOptions(this.writerProperties, conf);
            this.writerOptions.setSchema(this.vectorizer.getSchema());
        }

        return this.writerOptions;
    }
}
