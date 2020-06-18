//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.google.protobuf.CodedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.apache.flink.annotation.Internal;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.orc.CompressionCodec;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcProto.Footer;
import org.apache.orc.OrcProto.Metadata;
import org.apache.orc.OrcProto.PostScript;
import org.apache.orc.OrcProto.Stream;
import org.apache.orc.OrcProto.StripeFooter;
import org.apache.orc.OrcProto.RowIndex.Builder;
import org.apache.orc.PhysicalWriter;
import org.apache.orc.impl.HadoopShims;
import org.apache.orc.impl.OrcCodecPool;
import org.apache.orc.impl.OutStream;
import org.apache.orc.impl.StreamName;
import org.apache.orc.impl.WriterImpl;
import org.apache.orc.impl.StreamName.Area;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.orc.OrcFile.*;

@Internal
public class CustomPhysicalWriterImpl implements PhysicalWriter {
    private static final Logger LOG = LoggerFactory.getLogger(CustomPhysicalWriterImpl.class);
    private static final byte[] ZEROS = new byte[65536];
    private static final int HDFS_BUFFER_SIZE = 262144;
    private final OutStream writer;
    private final CodedOutputStream protobufWriter;
    private final CompressionKind compress;
    private final Map<StreamName, CustomPhysicalWriterImpl.BufferedStream> streams;
    private final HadoopShims shims;
    private final int maxPadding;
    private final int bufferSize;
    private final long blockSize;
    private final boolean addBlockPadding;
    private final boolean writeVariableLengthBlocks;
    private CompressionCodec codec;
    private FSDataOutputStream out;
    private long headerLength;
    private long stripeStart;
    private long blockOffset;
    private int metadataLength;
    private int footerLength;

    public CustomPhysicalWriterImpl(FSDataOutputStream out, WriterOptions opts) throws IOException {
        if (opts.isEnforceBufferSize()) {
            this.bufferSize = opts.getBufferSize();
        } else {
            this.bufferSize = WriterImpl.getEstimatedBufferSize(opts.getStripeSize(), opts.getSchema().getMaximumId() + 1, opts.getBufferSize());
        }

        this.out = out;
        this.blockOffset = 0L;
        this.blockSize = opts.getBlockSize();
        this.maxPadding = (int)(opts.getPaddingTolerance() * (double)opts.getBufferSize());
        this.compress = opts.getCompress();
        this.codec = OrcCodecPool.getCodec(this.compress);
        this.streams = new TreeMap();
        this.writer = new OutStream("metadata", this.bufferSize, this.codec, new CustomPhysicalWriterImpl.DirectStream(this.out));
        this.shims = opts.getHadoopShims();
        this.addBlockPadding = opts.getBlockPadding();
        this.protobufWriter = CodedOutputStream.newInstance(this.writer);
        this.writeVariableLengthBlocks = opts.getWriteVariableLengthBlocks();
    }

    public void writeHeader() throws IOException {
        this.out.write("ORC".getBytes());
        this.headerLength = this.out.getPos();
    }

    public OutputReceiver createDataStream(StreamName name) throws IOException {
        CustomPhysicalWriterImpl.BufferedStream result = (CustomPhysicalWriterImpl.BufferedStream)this.streams.get(name);
        if (result == null) {
            result = new CustomPhysicalWriterImpl.BufferedStream();
            this.streams.put(name, result);
        }

        return result;
    }

    public void writeIndex(StreamName name, Builder index, CompressionCodec codec) throws IOException {
        OutputStream stream = new OutStream(this.toString(), this.bufferSize, codec, this.createDataStream(name));
        index.build().writeTo(stream);
        stream.flush();
    }

    public void writeBloomFilter(StreamName name, org.apache.orc.OrcProto.BloomFilterIndex.Builder bloom, CompressionCodec codec) throws IOException {
        OutputStream stream = new OutStream(this.toString(), this.bufferSize, codec, this.createDataStream(name));
        bloom.build().writeTo(stream);
        stream.flush();
    }

    public void finalizeStripe(org.apache.orc.OrcProto.StripeFooter.Builder footerBuilder, org.apache.orc.OrcProto.StripeInformation.Builder dirEntry) throws IOException {
        long indexSize = 0L;
        long dataSize = 0L;
        Iterator var7 = this.streams.entrySet().iterator();

        while(var7.hasNext()) {
            Entry<StreamName, CustomPhysicalWriterImpl.BufferedStream> pair = (Entry)var7.next();
            CustomPhysicalWriterImpl.BufferedStream receiver = (CustomPhysicalWriterImpl.BufferedStream)pair.getValue();

            if (!receiver.isSuppressed) {
                long streamSize = receiver.getOutputSize();
                StreamName name = (StreamName)pair.getKey();
                footerBuilder.addStreams(Stream.newBuilder().setColumn(name.getColumn()).setKind(name.getKind()).setLength(streamSize));
                if (Area.INDEX == name.getArea()) {
                    indexSize += streamSize;
                } else {
                    dataSize += streamSize;
                }
            }
        }

        dirEntry.setIndexLength(indexSize).setDataLength(dataSize);
        StripeFooter footer = footerBuilder.build();
        this.padStripe(indexSize + dataSize + (long)footer.getSerializedSize());
        Iterator var14 = this.streams.entrySet().iterator();

        while(var14.hasNext()) {
            Entry<StreamName, CustomPhysicalWriterImpl.BufferedStream> pair = (Entry)var14.next();
            ((CustomPhysicalWriterImpl.BufferedStream)pair.getValue()).spillToDiskAndClear(this.out);
        }

        this.writeStripeFooter(footer, dataSize, indexSize, dirEntry);
    }

    public void writeFileMetadata(org.apache.orc.OrcProto.Metadata.Builder builder) throws IOException {
        long startPosition = this.out.getPos();
        Metadata metadata = builder.build();
        metadata.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        this.metadataLength = (int)(this.out.getPos() - startPosition);
    }

    public void writeFileFooter(org.apache.orc.OrcProto.Footer.Builder builder) throws IOException {
        long bodyLength = this.out.getPos() - (long)this.metadataLength;
        builder.setContentLength(bodyLength);
        builder.setHeaderLength(this.headerLength);
        long startPosition = this.out.getPos();
        Footer footer = builder.build();
        footer.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        this.footerLength = (int)(this.out.getPos() - startPosition);
    }

    public long writePostScript(org.apache.orc.OrcProto.PostScript.Builder builder) throws IOException {
        builder.setFooterLength((long)this.footerLength);
        builder.setMetadataLength((long)this.metadataLength);
        PostScript ps = builder.build();
        long startPosition = this.out.getPos();
        ps.writeTo(this.out);
        long length = this.out.getPos() - startPosition;
        if (length > 255L) {
            throw new IllegalArgumentException("PostScript too large at " + length);
        } else {
            this.out.write((int)length);
            return this.out.getPos();
        }
    }

    public void close() {
        OrcCodecPool.returnCodec(this.compress, this.codec);
        this.codec = null;
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void appendRawStripe(ByteBuffer buffer, org.apache.orc.OrcProto.StripeInformation.Builder dirEntry) throws IOException {
        long start = this.out.getPos();
        int length = buffer.remaining();
        long availBlockSpace = this.blockSize - start % this.blockSize;
        if ((long)length < this.blockSize && (long)length > availBlockSpace && this.addBlockPadding) {
            byte[] pad = new byte[(int)Math.min(262144L, availBlockSpace)];
            LOG.info(String.format("Padding ORC by %d bytes while merging..", availBlockSpace));

            int writeLen;
            for(start += availBlockSpace; availBlockSpace > 0L; availBlockSpace -= (long)writeLen) {
                writeLen = (int)Math.min(availBlockSpace, (long)pad.length);
                this.out.write(pad, 0, writeLen);
            }
        }

        this.out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), length);
        dirEntry.setOffset(start);
    }

    public CompressionCodec getCompressionCodec() {
        return this.codec;
    }

    public long getFileBytes(int column) {
        long size = 0L;
        Iterator var4 = this.streams.entrySet().iterator();

        while(var4.hasNext()) {
            Entry<StreamName, CustomPhysicalWriterImpl.BufferedStream> pair = (Entry)var4.next();
            CustomPhysicalWriterImpl.BufferedStream receiver = (CustomPhysicalWriterImpl.BufferedStream)pair.getValue();
            if (!receiver.isSuppressed) {
                StreamName name = (StreamName)pair.getKey();
                if (name.getColumn() == column && name.getArea() != Area.INDEX) {
                    size += receiver.getOutputSize();
                }
            }
        }

        return size;
    }

    private void padStripe(long stripeSize) throws IOException {
        this.stripeStart = this.out.getPos();
        long previousBytesInBlock = (this.stripeStart - this.blockOffset) % this.blockSize;
        if (previousBytesInBlock > 0L && previousBytesInBlock + stripeSize >= this.blockSize) {
            if (this.writeVariableLengthBlocks && this.shims.endVariableLengthBlock(this.out)) {
                this.blockOffset = this.stripeStart;
            } else if (this.addBlockPadding) {
                long padding = this.blockSize - previousBytesInBlock;
                if (padding <= (long)this.maxPadding) {
                    writeZeros(this.out, padding);
                    this.stripeStart += padding;
                }
            }
        }

    }

    private void writeStripeFooter(StripeFooter footer, long dataSize, long indexSize, org.apache.orc.OrcProto.StripeInformation.Builder dirEntry) throws IOException {
        footer.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        System.out.println("writeStripeFooter");
        dirEntry.setOffset(this.stripeStart);
        dirEntry.setFooterLength(this.out.getPos() - this.stripeStart - dataSize - indexSize);

    }

    private static void writeZeros(OutputStream output, long remaining) throws IOException {
        while(remaining > 0L) {
            long size = Math.min((long)ZEROS.length, remaining);
            output.write(ZEROS, 0, (int)size);
            remaining -= size;
        }

    }

    private static final class BufferedStream implements OutputReceiver {
        private boolean isSuppressed;
        private final List<ByteBuffer> output;

        private BufferedStream() {
            this.isSuppressed = false;
            this.output = new ArrayList();
        }

        public void output(ByteBuffer buffer) {
            if (!this.isSuppressed) {
                this.output.add(buffer);
            }

        }

        public void suppress() {
            this.isSuppressed = true;
            this.output.clear();
        }

        void spillToDiskAndClear(FSDataOutputStream raw) throws IOException {
            if (!this.isSuppressed) {
                Iterator var2 = this.output.iterator();

                while(var2.hasNext()) {
                    ByteBuffer buffer = (ByteBuffer)var2.next();
                    raw.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
                }

                this.output.clear();
            }

            this.isSuppressed = false;
        }

        public long getOutputSize() {
            long result = 0L;

            ByteBuffer buffer;
            for(Iterator var3 = this.output.iterator(); var3.hasNext(); result += (long)buffer.remaining()) {
                buffer = (ByteBuffer)var3.next();
            }

            return result;
        }
    }

    private static class DirectStream implements OutputReceiver {
        private final FSDataOutputStream output;

        DirectStream(FSDataOutputStream output) {
            this.output = output;
        }

        public void output(ByteBuffer buffer) throws IOException {
            this.output.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        }

        public void suppress() {
            throw new UnsupportedOperationException("Can't suppress direct stream");
        }
    }
}
