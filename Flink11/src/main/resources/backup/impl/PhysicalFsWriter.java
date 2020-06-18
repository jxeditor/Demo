//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.orc.impl;

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

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.orc.CompressionCodec;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcProto.Footer;
import org.apache.orc.OrcProto.Metadata;
import org.apache.orc.OrcProto.PostScript;
import org.apache.orc.OrcProto.Stream;
import org.apache.orc.OrcProto.StripeFooter;
import org.apache.orc.OrcProto.StripeInformation.Builder;
import org.apache.orc.PhysicalWriter;
import org.apache.orc.impl.StreamName.Area;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.orc.OrcFile.*;


public class PhysicalFsWriter implements PhysicalWriter {
    private static final Logger LOG = LoggerFactory.getLogger(PhysicalFsWriter.class);
    private static final int HDFS_BUFFER_SIZE = 262144;
    private static final byte[] ZEROS = new byte[65536];
    private final Path path;
    private final HadoopShims shims;
    private final long blockSize;
    private final int bufferSize;
    private final int maxPadding;
    private final CompressionKind compress;
    private final boolean addBlockPadding;
    private final boolean writeVariableLengthBlocks;
    private final Map<StreamName, PhysicalFsWriter.BufferedStream> streams = new TreeMap();
    private FSDataOutputStream rawWriter;
    private OutStream writer;
    private CodedOutputStream protobufWriter;
    private CompressionCodec codec;
    private long headerLength;
    private long stripeStart;
    private long blockOffset;
    private int metadataLength;
    private int footerLength;

    public PhysicalFsWriter(FileSystem fs, Path path, WriterOptions opts) throws IOException {
        this.path = path;
        long defaultStripeSize = opts.getStripeSize();
        this.addBlockPadding = opts.getBlockPadding();
        if (opts.isEnforceBufferSize()) {
            this.bufferSize = opts.getBufferSize();
        } else {
            this.bufferSize = WriterImpl.getEstimatedBufferSize(defaultStripeSize, opts.getSchema().getMaximumId() + 1, opts.getBufferSize());
        }

        this.compress = opts.getCompress();
        this.maxPadding = (int) (opts.getPaddingTolerance() * (double) defaultStripeSize);
        this.blockSize = opts.getBlockSize();
        LOG.info("ORC writer created for path: {} with stripeSize: {} blockSize: {} compression: {} bufferSize: {}", new Object[]{path, defaultStripeSize, this.blockSize, this.compress, this.bufferSize});
        this.rawWriter = fs.create(path, opts.getOverwrite(), 262144, fs.getDefaultReplication(path), this.blockSize);
        this.blockOffset = 0L;
        this.codec = OrcCodecPool.getCodec(this.compress);
        this.writer = new OutStream("metadata", this.bufferSize, this.codec, new PhysicalFsWriter.DirectStream(this.rawWriter));
        this.protobufWriter = CodedOutputStream.newInstance(this.writer);
        this.writeVariableLengthBlocks = opts.getWriteVariableLengthBlocks();
        this.shims = opts.getHadoopShims();
    }

    private static void writeZeros(OutputStream output, long remaining) throws IOException {
        while (remaining > 0L) {
            long size = Math.min((long) ZEROS.length, remaining);
            output.write(ZEROS, 0, (int) size);
            remaining -= size;
        }

    }

    public CompressionCodec getCompressionCodec() {
        return this.codec;
    }

    public long getFileBytes(int column) {
        long size = 0L;
        Iterator var4 = this.streams.entrySet().iterator();

        while (var4.hasNext()) {
            Entry<StreamName, PhysicalFsWriter.BufferedStream> pair = (Entry) var4.next();
            PhysicalFsWriter.BufferedStream receiver = (PhysicalFsWriter.BufferedStream) pair.getValue();
            if (!receiver.isSuppressed) {
                StreamName name = (StreamName) pair.getKey();
                if (name.getColumn() == column && name.getArea() != Area.INDEX) {
                    size += receiver.getOutputSize();
                }
            }
        }

        return size;
//        return 0L;
    }

    private void padStripe(long stripeSize) throws IOException {
        this.stripeStart = this.rawWriter.getPos();
        long previousBytesInBlock = (this.stripeStart - this.blockOffset) % this.blockSize;
        if (previousBytesInBlock > 0L && previousBytesInBlock + stripeSize >= this.blockSize) {
            if (this.writeVariableLengthBlocks && this.shims.endVariableLengthBlock(this.rawWriter)) {
                this.blockOffset = this.stripeStart;
            } else if (this.addBlockPadding) {
                long padding = this.blockSize - previousBytesInBlock;
                if (padding <= (long) this.maxPadding) {
                    writeZeros(this.rawWriter, padding);
                    this.stripeStart += padding;
                }
            }
        }

    }

    private void writeStripeFooter(StripeFooter footer, long dataSize, long indexSize, Builder dirEntry) throws IOException {
        footer.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        dirEntry.setOffset(this.stripeStart);
        dirEntry.setFooterLength(this.rawWriter.getPos() - this.stripeStart - dataSize - indexSize);
    }

    public void writeFileMetadata(org.apache.orc.OrcProto.Metadata.Builder builder) throws IOException {
        long startPosn = this.rawWriter.getPos();
        Metadata metadata = builder.build();
        metadata.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        this.metadataLength = (int) (this.rawWriter.getPos() - startPosn);
    }

    public void writeFileFooter(org.apache.orc.OrcProto.Footer.Builder builder) throws IOException {
        long bodyLength = this.rawWriter.getPos() - (long) this.metadataLength;
        builder.setContentLength(bodyLength);
        builder.setHeaderLength(this.headerLength);
        long startPosn = this.rawWriter.getPos();
        Footer footer = builder.build();
        footer.writeTo(this.protobufWriter);
        this.protobufWriter.flush();
        this.writer.flush();
        this.footerLength = (int) (this.rawWriter.getPos() - startPosn);
    }

    public long writePostScript(org.apache.orc.OrcProto.PostScript.Builder builder) throws IOException {
        builder.setFooterLength((long) this.footerLength);
        builder.setMetadataLength((long) this.metadataLength);
        PostScript ps = builder.build();
        long startPosn = this.rawWriter.getPos();
        ps.writeTo(this.rawWriter);
        long length = this.rawWriter.getPos() - startPosn;
        if (length > 255L) {
            throw new IllegalArgumentException("PostScript too large at " + length);
        } else {
            this.rawWriter.writeByte((int) length);
            return this.rawWriter.getPos();
        }
    }

    public void close() throws IOException {
        OrcCodecPool.returnCodec(this.compress, this.codec);
        this.codec = null;
        this.rawWriter.close();
        this.rawWriter = null;
    }

    public void flush() throws IOException {
        this.rawWriter.hflush();
    }

    public void appendRawStripe(ByteBuffer buffer, Builder dirEntry) throws IOException {
        long start = this.rawWriter.getPos();
        int length = buffer.remaining();
        long availBlockSpace = this.blockSize - start % this.blockSize;
        if ((long) length < this.blockSize && (long) length > availBlockSpace && this.addBlockPadding) {
            byte[] pad = new byte[(int) Math.min(262144L, availBlockSpace)];
            LOG.info(String.format("Padding ORC by %d bytes while merging..", availBlockSpace));

            int writeLen;
            for (start += availBlockSpace; availBlockSpace > 0L; availBlockSpace -= (long) writeLen) {
                writeLen = (int) Math.min(availBlockSpace, (long) pad.length);
                this.rawWriter.write(pad, 0, writeLen);
            }
        }

        this.rawWriter.write(buffer.array(), buffer.arrayOffset() + buffer.position(), length);
        dirEntry.setOffset(start);
    }

    public void finalizeStripe(org.apache.orc.OrcProto.StripeFooter.Builder footerBuilder, Builder dirEntry) throws IOException {
        long indexSize = 0L;
        long dataSize = 0L;
        Iterator var7 = this.streams.entrySet().iterator();

        while (var7.hasNext()) {
            Entry<StreamName, PhysicalFsWriter.BufferedStream> pair = (Entry) var7.next();
            PhysicalFsWriter.BufferedStream receiver = (PhysicalFsWriter.BufferedStream) pair.getValue();
            if (!receiver.isSuppressed) {
                long streamSize = receiver.getOutputSize();
                StreamName name = (StreamName) pair.getKey();
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
        this.padStripe(indexSize + dataSize + (long) footer.getSerializedSize());
        Iterator var14 = this.streams.entrySet().iterator();

        while (var14.hasNext()) {
            Entry<StreamName, PhysicalFsWriter.BufferedStream> pair = (Entry) var14.next();
            ((PhysicalFsWriter.BufferedStream) pair.getValue()).spillToDiskAndClear(this.rawWriter);
        }

        this.writeStripeFooter(footer, dataSize, indexSize, dirEntry);
    }

    public void writeHeader() throws IOException {
        this.rawWriter.writeBytes("ORC");
        this.headerLength = this.rawWriter.getPos();
    }

    public PhysicalFsWriter.BufferedStream createDataStream(StreamName name) {
        PhysicalFsWriter.BufferedStream result = (PhysicalFsWriter.BufferedStream) this.streams.get(name);
        if (result == null) {
            result = new PhysicalFsWriter.BufferedStream();
            this.streams.put(name, result);
        }

        return result;
    }


    public void writeIndex(StreamName name, org.apache.orc.OrcProto.RowIndex.Builder index, CompressionCodec codec) throws IOException {
        OutputStream stream = new OutStream(this.path.toString(), this.bufferSize, codec, this.createDataStream(name));
        index.build().writeTo(stream);
        stream.flush();
    }

    public void writeBloomFilter(StreamName name, org.apache.orc.OrcProto.BloomFilterIndex.Builder bloom, CompressionCodec codec) throws IOException {
        OutputStream stream = new OutStream(this.path.toString(), this.bufferSize, codec, this.createDataStream(name));
        bloom.build().writeTo(stream);
        stream.flush();
    }

    public String toString() {
        return this.path.toString();
    }

    private static final class BufferedStream implements OutputReceiver {
        private final List<ByteBuffer> output;
        private boolean isSuppressed;

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

                while (var2.hasNext()) {
                    ByteBuffer buffer = (ByteBuffer) var2.next();
                    raw.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
                }

                this.output.clear();
            }

            this.isSuppressed = false;
        }

        public long getOutputSize() {
            long result = 0L;

            ByteBuffer buffer;
            for (Iterator var3 = this.output.iterator(); var3.hasNext(); result += (long) buffer.remaining()) {
                buffer = (ByteBuffer) var3.next();
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
