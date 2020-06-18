import org.apache.flink.orc.vector.Vectorizer;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import java.io.IOException;

/**
 * @author XiaShuai on 2020/6/15.
 */
public class DemoVectorizer extends Vectorizer<Demo> {

    public DemoVectorizer(String schema) {
        super(schema);
    }

    @Override
    public void vectorize(Demo demo, VectorizedRowBatch vectorizedRowBatch) throws IOException {
        int id = vectorizedRowBatch.size++;
        System.out.println(vectorizedRowBatch.size);
        for (int i = 0; i < 3; ++i) {
            BytesColumnVector vector = (BytesColumnVector) vectorizedRowBatch.cols[i];
            byte[] bytes = demo.platform().getBytes();
            vector.setVal(id, bytes, 0, bytes.length);
        }
    }
}
