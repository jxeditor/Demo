//
//public class StreamExecCalc$8 extends org.apache.flink.table.runtime.operators.AbstractProcessStreamOperator
//        implements org.apache.flink.streaming.api.operators.OneInputStreamOperator {
//
//    private final Object[] references;
//
//    org.apache.flink.table.data.DecimalData decimal$3 =
//            org.apache.flink.table.data.DecimalDataUtils.castFrom("0.4", 2, 1);
//
//
//    org.apache.flink.table.data.DecimalData decimal$4 =
//            org.apache.flink.table.data.DecimalDataUtils.castFrom("0.5", 2, 1);
//
//
//    org.apache.flink.table.data.DecimalData decimal$5 =
//            org.apache.flink.table.data.DecimalDataUtils.castFrom("0.06", 3, 2);
//
//
//    org.apache.flink.table.data.binary.BinaryArrayData array$6 = new org.apache.flink.table.data.binary.BinaryArrayData();
//    org.apache.flink.table.data.writer.BinaryArrayWriter writer$7 = new org.apache.flink.table.data.writer.BinaryArrayWriter(array$6, 3, 8);
//
//    org.apache.flink.table.data.BoxedWrapperRowData out = new org.apache.flink.table.data.BoxedWrapperRowData(1);
//    private final org.apache.flink.streaming.runtime.streamrecord.StreamRecord outElement = new org.apache.flink.streaming.runtime.streamrecord.StreamRecord(null);
//
//    public StreamExecCalc$8(
//            Object[] references,
//            org.apache.flink.streaming.runtime.tasks.StreamTask task,
//            org.apache.flink.streaming.api.graph.StreamConfig config,
//            org.apache.flink.streaming.api.operators.Output output,
//            org.apache.flink.streaming.runtime.tasks.ProcessingTimeService processingTimeService) throws Exception {
//        this.references = references;
//
//        writer$7.reset();
//
//
//        if (false) {
//            writer$7.setNullLong(0);
//        } else {
//            writer$7.writeDecimal(0, ((org.apache.flink.table.data.DecimalData) decimal$3), 3);
//        }
//
//
//
//        if (false) {
//            writer$7.setNullLong(1);
//        } else {
//            writer$7.writeDecimal(1, ((org.apache.flink.table.data.DecimalData) decimal$4), 3);
//        }
//
//
//
//        if (false) {
//            writer$7.setNullLong(2);
//        } else {
//            writer$7.writeDecimal(2, ((org.apache.flink.table.data.DecimalData) decimal$5), 3);
//        }
//
//        writer$7.complete();
//
//        this.setup(task, config, output);
//        if (this instanceof org.apache.flink.streaming.api.operators.AbstractStreamOperator) {
//            ((org.apache.flink.streaming.api.operators.AbstractStreamOperator) this)
//                    .setProcessingTimeService(processingTimeService);
//        }
//    }
//
//    @Override
//    public void open() throws Exception {
//        super.open();
//
//    }
//
//    @Override
//    public void processElement(org.apache.flink.streaming.runtime.streamrecord.StreamRecord element) throws Exception {
//        org.apache.flink.table.data.RowData in1 = (org.apache.flink.table.data.RowData) element.getValue();
//
//
//
//
//
//
//        out.setRowKind(in1.getRowKind());
//
//
//
//
//        if (false) {
//            out.setNullAt(0);
//        } else {
//            out.setNonPrimitiveValue(0, array$6);
//        }
//
//
//        output.collect(outElement.replace(out));
//
//
//    }
//
//
//
//    @Override
//    public void close() throws Exception {
//        super.close();
//
//    }
//
//
//}
//

import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.DecimalDataUtils;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryArrayData;
import org.apache.flink.table.data.conversion.DataStructureConverter;
import org.apache.flink.table.data.conversion.DataStructureConverters;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.factories.PrintTableSinkFactory;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

import java.math.BigDecimal;
import java.util.stream.IntStream;

public class Test{
    public static void main(String[] args) {
        BinaryArrayData binaryArrayData = BinaryArrayData.fromPrimitiveArray(new double[]{0.4, 0.5, 0.06});

        binaryArrayData.setDecimal(0, DecimalDataUtils.castFrom("0.4", 2, 1),2);
        binaryArrayData.setDecimal(1, DecimalDataUtils.castFrom("0.5", 2, 1),2);
        binaryArrayData.setDecimal(2, DecimalDataUtils.castFrom("0.06", 3, 2),3);

        DecimalData decimal = binaryArrayData.getDecimal(0, 3, 2);
        System.out.println(decimal.toBigDecimal());

    }
}