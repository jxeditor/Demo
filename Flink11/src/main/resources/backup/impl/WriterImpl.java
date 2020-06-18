//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.orc.impl;

import com.google.protobuf.ByteString;
import io.airlift.compress.lz4.Lz4Compressor;
import io.airlift.compress.lz4.Lz4Decompressor;
import io.airlift.compress.lzo.LzoCompressor;
import io.airlift.compress.lzo.LzoDecompressor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.orc.*;
import org.apache.orc.CompressionCodec.Modifier;
import org.apache.orc.MemoryManager;
import org.apache.orc.MemoryManager.Callback;
import org.apache.orc.OrcProto.Footer;
import org.apache.orc.OrcProto.Metadata;
import org.apache.orc.OrcProto.PostScript;
import org.apache.orc.OrcProto.StripeFooter;
import org.apache.orc.OrcProto.StripeInformation;
import org.apache.orc.OrcProto.StripeStatistics;
import org.apache.orc.OrcProto.UserMetadataItem;
import org.apache.orc.OrcProto.Metadata.Builder;
import org.apache.orc.OrcProto.Stream.Kind;
import org.apache.orc.TypeDescription.Category;
import org.apache.orc.impl.writer.TreeWriter;
import org.apache.orc.impl.writer.TreeWriter.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.orc.OrcFile.*;

public class WriterImpl implements WriterInternal, Callback {
    private static final Logger LOG = LoggerFactory.getLogger(WriterImpl.class);
    private static final int MIN_ROW_INDEX_STRIDE = 1000;
    private final Path path;
    private final int rowIndexStride;
    private final CompressionKind compress;
    private final TypeDescription schema;
    private final PhysicalWriter physicalWriter;
    private final WriterVersion writerVersion;
    private final List<StripeInformation> stripes = new ArrayList();
    private final Builder fileMetadata = Metadata.newBuilder();
    private final Map<String, ByteString> userMetadata = new TreeMap();
    private final TreeWriter treeWriter;
    private final boolean buildIndex;
    private final MemoryManager memoryManager;
    private final Version version;
    private final Configuration conf;
    private final WriterCallback callback;
    private final WriterContext callbackContext;
    private final EncodingStrategy encodingStrategy;
    private final CompressionStrategy compressionStrategy;
    private final boolean[] bloomFilterColumns;
    private final double bloomFilterFpp;
    private final BloomFilterVersion bloomFilterVersion;
    private final boolean writeTimeZone;
    private final boolean useUTCTimeZone;
    private final double dictionaryKeySizeThreshold;
    private final boolean[] directEncodingColumns;
    private long adjustedStripeSize;
    private int bufferSize;
    private long rowCount = 0L;
    private long rowsInStripe = 0L;
    private long rawDataSize = 0L;
    private int rowsInIndex = 0;
    private long lastFlushOffset = 0L;
    private int stripesAtLastFlush = -1;

    public WriterImpl(FileSystem fs, Path path, WriterOptions opts) throws IOException {
        this.path = path;
        this.conf = opts.getConfiguration();
        this.callback = opts.getCallback();
        this.schema = opts.getSchema();
        this.writerVersion = opts.getWriterVersion();
        this.bloomFilterVersion = opts.getBloomFilterVersion();
        this.directEncodingColumns = OrcUtils.includeColumns(opts.getDirectEncodingColumns(), opts.getSchema());
        this.dictionaryKeySizeThreshold = OrcConf.DICTIONARY_KEY_SIZE_THRESHOLD.getDouble(this.conf);
        if (this.callback != null) {
            this.callbackContext = new WriterContext() {
                public Writer getWriter() {
                    return WriterImpl.this;
                }
            };
        } else {
            this.callbackContext = null;
        }

        this.writeTimeZone = hasTimestamp(this.schema);
        this.useUTCTimeZone = opts.getUseUTCTimestamp();
        this.adjustedStripeSize = opts.getStripeSize();
        this.version = opts.getVersion();
        this.encodingStrategy = opts.getEncodingStrategy();
        this.compressionStrategy = opts.getCompressionStrategy();
        this.compress = opts.getCompress();
        this.rowIndexStride = opts.getRowIndexStride();
        this.memoryManager = opts.getMemoryManager();
        this.buildIndex = this.rowIndexStride > 0;
        int numColumns = this.schema.getMaximumId() + 1;
        if (opts.isEnforceBufferSize()) {
            OutStream.assertBufferSizeValid(opts.getBufferSize());
            this.bufferSize = opts.getBufferSize();
        } else {
            this.bufferSize = getEstimatedBufferSize(this.adjustedStripeSize, numColumns, opts.getBufferSize());
        }

        if (this.version == Version.FUTURE) {
            throw new IllegalArgumentException("Can not write in a unknown version.");
        } else {
            if (this.version == Version.UNSTABLE_PRE_2_0) {
                LOG.warn("ORC files written in " + this.version.getName() + " will not be readable by other versions of the software. It is only for developer testing.");
            }

            if (this.version == Version.V_0_11) {
                this.bloomFilterColumns = new boolean[this.schema.getMaximumId() + 1];
            } else {
                this.bloomFilterColumns = OrcUtils.includeColumns(opts.getBloomFilterColumns(), this.schema);
            }

            this.bloomFilterFpp = opts.getBloomFilterFpp();
            this.physicalWriter = (PhysicalWriter) (opts.getPhysicalWriter() == null ? new PhysicalFsWriter(fs, path, opts) : opts.getPhysicalWriter());
            this.physicalWriter.writeHeader();
            this.treeWriter = Factory.create(this.schema, new WriterImpl.StreamFactory(), false);
            if (this.buildIndex && this.rowIndexStride < 1000) {
                throw new IllegalArgumentException("Row stride must be at least 1000");
            } else {
                this.memoryManager.addWriter(path, opts.getStripeSize(), this);
                LOG.info("ORC writer created for path: {} with stripeSize: {} blockSize: {} compression: {} bufferSize: {}", new Object[]{path, this.adjustedStripeSize, opts.getBlockSize(), this.compress, this.bufferSize});
            }
        }
    }

    public static int getEstimatedBufferSize(long stripeSize, int numColumns, int bs) {
        int estBufferSize = (int) (stripeSize / (20L * (long) numColumns));
        estBufferSize = getClosestBufferSize(estBufferSize);
        return estBufferSize > bs ? bs : estBufferSize;
    }

    private static int getClosestBufferSize(int estBufferSize) {
        int kb4 = 1;
        int kb8 = 1;
        int kb16 = 1;
        int kb32 = '耀';
        int kb64 = 65536;
        int kb128 = 131072;
        int kb256 = 262144;
        if (estBufferSize <= 4096) {
            return 4096;
        } else if (estBufferSize <= 8192) {
            return 8192;
        } else if (estBufferSize <= 16384) {
            return 16384;
        } else if (estBufferSize <= 32768) {
            return 32768;
        } else if (estBufferSize <= 65536) {
            return 65536;
        } else {
            return estBufferSize <= 131072 ? 131072 : 262144;
        }
    }

    public static CompressionCodec createCodec(CompressionKind kind) {
        switch (kind) {
            case NONE:
                return null;
            case ZLIB:
                return new ZlibCodec();
            case SNAPPY:
                return new SnappyCodec();
            case LZO:
                return new AircompressorCodec(new LzoCompressor(), new LzoDecompressor());
            case LZ4:
                return new AircompressorCodec(new Lz4Compressor(), new Lz4Decompressor());
            default:
                throw new IllegalArgumentException("Unknown compression codec: " + kind);
        }
    }

    private static void writeTypes(org.apache.orc.OrcProto.Footer.Builder builder, TypeDescription schema) {
        builder.addAllTypes(OrcUtils.getOrcTypes(schema));
    }

    static void checkArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean hasTimestamp(TypeDescription schema) {
        if (schema.getCategory() == Category.TIMESTAMP) {
            return true;
        } else {
            List<TypeDescription> children = schema.getChildren();
            if (children != null) {
                Iterator var2 = children.iterator();

                while (var2.hasNext()) {
                    TypeDescription child = (TypeDescription) var2.next();
                    if (hasTimestamp(child)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void increaseCompressionSize(int newSize) {
        if (newSize > this.bufferSize) {
            this.bufferSize = newSize;
        }

    }

    public boolean checkMemory(double newScale) throws IOException {
        long limit = Math.round((double) this.adjustedStripeSize * newScale);
        long size = this.treeWriter.estimateMemory();
        if (LOG.isDebugEnabled()) {
            LOG.debug("ORC writer " + this.physicalWriter + " size = " + size + " limit = " + limit);
        }

        if (size > limit) {
            this.flushStripe();
            return true;
        } else {
            return false;
        }
    }

    CompressionCodec getCustomizedCodec(Kind kind) {
        CompressionCodec result = this.physicalWriter.getCompressionCodec();
        if (result != null) {
            switch (kind) {
                case BLOOM_FILTER:
                case DATA:
                case DICTIONARY_DATA:
                case BLOOM_FILTER_UTF8:
                    if (this.compressionStrategy == CompressionStrategy.SPEED) {
                        result = result.modify(EnumSet.of(Modifier.FAST, Modifier.TEXT));
                    } else {
                        result = result.modify(EnumSet.of(Modifier.DEFAULT, Modifier.TEXT));
                    }
                    break;
                case LENGTH:
                case DICTIONARY_COUNT:
                case PRESENT:
                case ROW_INDEX:
                case SECONDARY:
                    result = result.modify(EnumSet.of(Modifier.FASTEST, Modifier.BINARY));
                    break;
                default:
                    LOG.info("Missing ORC compression modifiers for " + kind);
            }
        }

        return result;
    }

    private void createRowIndexEntry() throws IOException {
        this.treeWriter.createRowIndexEntry();
        this.rowsInIndex = 0;
    }

    private void flushStripe() throws IOException {
        if (this.buildIndex && this.rowsInIndex != 0) {
            this.createRowIndexEntry();
        }

        if (this.rowsInStripe != 0L) {
            if (this.callback != null) {
                this.callback.preStripeWrite(this.callbackContext);
            }

            int requiredIndexEntries = this.rowIndexStride == 0 ? 0 : (int) ((this.rowsInStripe + (long) this.rowIndexStride - 1L) / (long) this.rowIndexStride);
            org.apache.orc.OrcProto.StripeFooter.Builder builder = StripeFooter.newBuilder();
            if (this.writeTimeZone) {
                if (this.useUTCTimeZone) {
                    builder.setWriterTimezone("UTC");
                } else {
                    builder.setWriterTimezone(TimeZone.getDefault().getID());
                }
            }

            org.apache.orc.OrcProto.StripeStatistics.Builder stats = StripeStatistics.newBuilder();
            this.treeWriter.flushStreams();
            this.treeWriter.writeStripe(builder, stats, requiredIndexEntries);
            org.apache.orc.OrcProto.StripeInformation.Builder dirEntry = StripeInformation.newBuilder().setNumberOfRows(this.rowsInStripe);
            this.physicalWriter.finalizeStripe(builder, dirEntry);
            this.fileMetadata.addStripeStats(stats.build());
            this.stripes.add(dirEntry.build());
            this.rowCount += this.rowsInStripe;
            this.rowsInStripe = 0L;
        }

    }

    private long computeRawDataSize() {
        return this.treeWriter.getRawDataSize();
    }

    private org.apache.orc.OrcProto.CompressionKind writeCompressionKind(CompressionKind kind) {
        switch (kind) {
            case NONE:
                return org.apache.orc.OrcProto.CompressionKind.NONE;
            case ZLIB:
                return org.apache.orc.OrcProto.CompressionKind.ZLIB;
            case SNAPPY:
                return org.apache.orc.OrcProto.CompressionKind.SNAPPY;
            case LZO:
                return org.apache.orc.OrcProto.CompressionKind.LZO;
            case LZ4:
                return org.apache.orc.OrcProto.CompressionKind.LZ4;
            default:
                throw new IllegalArgumentException("Unknown compression " + kind);
        }
    }

    private void writeFileStatistics(org.apache.orc.OrcProto.Footer.Builder builder, TreeWriter writer) throws IOException {
        writer.writeFileStatistics(builder);
    }

    private void writeMetadata() throws IOException {
        List<StripeStatistics> stripeStatsList = this.fileMetadata.getStripeStatsList();
        for (StripeStatistics ss : stripeStatsList) {
            int serializedSize = ss.getSerializedSize();
            System.out.println(serializedSize);
        }
        this.physicalWriter.writeFileMetadata(this.fileMetadata);
    }

    private long writePostScript() throws IOException {
        org.apache.orc.OrcProto.PostScript.Builder builder = PostScript.newBuilder().setCompression(this.writeCompressionKind(this.compress)).setMagic("ORC").addVersion(this.version.getMajor()).addVersion(this.version.getMinor()).setWriterVersion(this.writerVersion.getId());
        if (this.compress != CompressionKind.NONE) {
            builder.setCompressionBlockSize((long) this.bufferSize);
        }

        return this.physicalWriter.writePostScript(builder);
    }

    private long writeFooter() throws IOException {
        this.writeMetadata();
        org.apache.orc.OrcProto.Footer.Builder builder = Footer.newBuilder();
        builder.setNumberOfRows(this.rowCount);
        builder.setRowIndexStride(this.rowIndexStride);
        this.rawDataSize = this.computeRawDataSize();
        writeTypes(builder, this.schema);
        Iterator var2 = this.stripes.iterator();

        while (var2.hasNext()) {
            StripeInformation stripe = (StripeInformation) var2.next();
            builder.addStripes(stripe);
        }

        this.writeFileStatistics(builder, this.treeWriter);
        var2 = this.userMetadata.entrySet().iterator();

        while (var2.hasNext()) {
            Entry<String, ByteString> entry = (Entry) var2.next();
            builder.addMetadata(UserMetadataItem.newBuilder().setName((String) entry.getKey()).setValue((ByteString) entry.getValue()));
        }

        builder.setWriter(WriterImplementation.ORC_JAVA.getId());
        this.physicalWriter.writeFileFooter(builder);
        return this.writePostScript();
    }

    public TypeDescription getSchema() {
        return this.schema;
    }

    public void addUserMetadata(String name, ByteBuffer value) {
        this.userMetadata.put(name, ByteString.copyFrom(value));
    }

    public void addRowBatch(VectorizedRowBatch batch) throws IOException {
        if (this.buildIndex) {
            int posn = 0;

            while (posn < batch.size) {
                int chunkSize = Math.min(batch.size - posn, this.rowIndexStride - this.rowsInIndex);
                this.treeWriter.writeRootBatch(batch, posn, chunkSize);
                posn += chunkSize;
                this.rowsInIndex += chunkSize;
                this.rowsInStripe += (long) chunkSize;
                if (this.rowsInIndex >= this.rowIndexStride) {
                    this.createRowIndexEntry();
                }
            }
        } else {
            this.rowsInStripe += (long) batch.size;
            this.treeWriter.writeRootBatch(batch, 0, batch.size);
        }

        this.memoryManager.addedRow(batch.size);
    }

    public void close() throws IOException {
        if (this.callback != null) {
            this.callback.preFooterWrite(this.callbackContext);
        }

        this.memoryManager.removeWriter(this.path);
        this.flushStripe();
        this.lastFlushOffset = this.writeFooter();
        this.physicalWriter.close();
    }

    public long getRawDataSize() {
        return this.rawDataSize;
    }

    public long getNumberOfRows() {
        return this.rowCount;
    }

    public long writeIntermediateFooter() throws IOException {
        this.flushStripe();
        if (this.stripesAtLastFlush != this.stripes.size()) {
            if (this.callback != null) {
                this.callback.preFooterWrite(this.callbackContext);
            }

            this.lastFlushOffset = this.writeFooter();
            this.stripesAtLastFlush = this.stripes.size();
            this.physicalWriter.flush();
        }

        return this.lastFlushOffset;
    }

    public void appendStripe(byte[] stripe, int offset, int length, org.apache.orc.StripeInformation stripeInfo, StripeStatistics stripeStatistics) throws IOException {
        checkArgument(stripe != null, "Stripe must not be null");
        checkArgument(length <= stripe.length, "Specified length must not be greater specified array length");
        checkArgument(stripeInfo != null, "Stripe information must not be null");
        checkArgument(stripeStatistics != null, "Stripe statistics must not be null");
        this.rowsInStripe = stripeInfo.getNumberOfRows();
        org.apache.orc.OrcProto.StripeInformation.Builder dirEntry = StripeInformation.newBuilder().setNumberOfRows(this.rowsInStripe).setIndexLength(stripeInfo.getIndexLength()).setDataLength(stripeInfo.getDataLength()).setFooterLength(stripeInfo.getFooterLength());
        this.physicalWriter.appendRawStripe(ByteBuffer.wrap(stripe, offset, length), dirEntry);
        this.treeWriter.updateFileStatistics(stripeStatistics);
        this.fileMetadata.addStripeStats(stripeStatistics);
        this.stripes.add(dirEntry.build());
        this.rowCount += this.rowsInStripe;
        this.rowsInStripe = 0L;
    }

    public void appendUserMetadata(List<UserMetadataItem> userMetadata) {
        if (userMetadata != null) {
            Iterator var2 = userMetadata.iterator();

            while (var2.hasNext()) {
                UserMetadataItem item = (UserMetadataItem) var2.next();
                this.userMetadata.put(item.getName(), item.getValue());
            }
        }

    }

    public ColumnStatistics[] getStatistics() throws IOException {
        org.apache.orc.OrcProto.Footer.Builder builder = Footer.newBuilder();
        this.writeFileStatistics(builder, this.treeWriter);
        return ReaderImpl.deserializeStats(this.schema, builder.getStatisticsList());
    }

    public CompressionCodec getCompressionCodec() {
        return this.physicalWriter.getCompressionCodec();
    }

    private class StreamFactory implements org.apache.orc.impl.writer.WriterContext {
        private StreamFactory() {
        }

        public OutStream createStream(int column, Kind kind) throws IOException {
            StreamName name = new StreamName(column, kind);
            CompressionCodec codec = WriterImpl.this.getCustomizedCodec(kind);
            return new OutStream(WriterImpl.this.physicalWriter.toString(), WriterImpl.this.bufferSize, codec, WriterImpl.this.physicalWriter.createDataStream(name));
        }

        public int getRowIndexStride() {
            return WriterImpl.this.rowIndexStride;
        }

        public boolean buildIndex() {
            return WriterImpl.this.buildIndex;
        }

        public boolean isCompressed() {
            return WriterImpl.this.physicalWriter.getCompressionCodec() != null;
        }

        public EncodingStrategy getEncodingStrategy() {
            return WriterImpl.this.encodingStrategy;
        }

        public boolean[] getBloomFilterColumns() {
            return WriterImpl.this.bloomFilterColumns;
        }

        public double getBloomFilterFPP() {
            return WriterImpl.this.bloomFilterFpp;
        }

        public Configuration getConfiguration() {
            return WriterImpl.this.conf;
        }

        public Version getVersion() {
            return WriterImpl.this.version;
        }

        public PhysicalWriter getPhysicalWriter() {
            return WriterImpl.this.physicalWriter;
        }

        public BloomFilterVersion getBloomFilterVersion() {
            return WriterImpl.this.bloomFilterVersion;
        }

        public void writeIndex(StreamName name, org.apache.orc.OrcProto.RowIndex.Builder index) throws IOException {
            WriterImpl.this.physicalWriter.writeIndex(name, index, WriterImpl.this.getCustomizedCodec(name.getKind()));
        }

        public void writeBloomFilter(StreamName name, org.apache.orc.OrcProto.BloomFilterIndex.Builder bloom) throws IOException {
            WriterImpl.this.physicalWriter.writeBloomFilter(name, bloom, WriterImpl.this.getCustomizedCodec(name.getKind()));
        }

        public boolean getUseUTCTimestamp() {
            return WriterImpl.this.useUTCTimeZone;
        }

        public double getDictionaryKeySizeThreshold(int columnId) {
            return WriterImpl.this.directEncodingColumns[columnId] ? 0.0D : WriterImpl.this.dictionaryKeySizeThreshold;
        }
    }
}
