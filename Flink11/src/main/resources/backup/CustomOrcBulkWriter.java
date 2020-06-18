//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.IOException;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.BulkWriter;
import org.apache.flink.orc.vector.Vectorizer;
import org.apache.flink.util.Preconditions;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.Writer;

@Internal
public class CustomOrcBulkWriter<T> implements BulkWriter<T> {
    private final Writer writer;
    private final Vectorizer<T> vectorizer;
    private final VectorizedRowBatch rowBatch;

    CustomOrcBulkWriter(Vectorizer<T> vectorizer, Writer writer) {
        this.vectorizer = (Vectorizer)Preconditions.checkNotNull(vectorizer);
        this.writer = (Writer)Preconditions.checkNotNull(writer);
        this.rowBatch = vectorizer.getSchema().createRowBatch();
        this.vectorizer.setWriter(this.writer);
    }

    public void addElement(T element) throws IOException {
        this.vectorizer.vectorize(element, this.rowBatch);
        if (this.rowBatch.size == this.rowBatch.getMaxSize()) {
            this.writer.addRowBatch(this.rowBatch);
            this.rowBatch.reset();
        }

    }

    public void flush() throws IOException {
        if (this.rowBatch.size != 0) {
            this.writer.addRowBatch(this.rowBatch);
            this.rowBatch.reset();
        }

    }

    public void finish() throws IOException {
        this.flush();
        this.writer.close();
    }
}
