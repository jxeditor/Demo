package com.test.flink;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.BasicArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.apache.flink.api.java.typeutils.ObjectArrayTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.formats.json.JsonRowSchemaConverter;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.TextNode;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.WrappingRuntimeException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author XiaShuai on 2020/6/11.
 */
public class CustomJsonRowDeserializationSchema implements DeserializationSchema<Row> {
    private static final long serialVersionUID = -228294330688809195L;
    private final RowTypeInfo typeInfo;
    private final ObjectMapper objectMapper;
    private boolean failOnMissingField;
    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter runtimeConverter;

    private CustomJsonRowDeserializationSchema(TypeInformation<Row> typeInfo, boolean failOnMissingField) {
        this.objectMapper = new ObjectMapper();
        Preconditions.checkNotNull(typeInfo, "Type information");
        Preconditions.checkArgument(typeInfo instanceof RowTypeInfo, "Only RowTypeInfo is supported");
        this.typeInfo = (RowTypeInfo) typeInfo;
        this.failOnMissingField = failOnMissingField;
        this.runtimeConverter = this.createConverter(this.typeInfo);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public CustomJsonRowDeserializationSchema(TypeInformation<Row> typeInfo) {
        this(typeInfo, false);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public CustomJsonRowDeserializationSchema(String jsonSchema) {
        this(JsonRowSchemaConverter.convert((String) Preconditions.checkNotNull(jsonSchema)), false);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setFailOnMissingField(boolean failOnMissingField) {
        this.failOnMissingField = failOnMissingField;
        this.runtimeConverter = this.createConverter(this.typeInfo);
    }

    public Row deserialize(byte[] message) throws IOException {
        try {
            JsonNode root = this.objectMapper.readTree(message);
            return (Row) this.runtimeConverter.convert(this.objectMapper, root);
        } catch (Throwable var3) {
//            throw new IOException("Failed to deserialize JSON object.", var3);
            System.out.println(new String(message));
        }
        return null;
    }

    public boolean isEndOfStream(Row nextElement) {
        return false;
    }

    public TypeInformation<Row> getProducedType() {
        return this.typeInfo;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            CustomJsonRowDeserializationSchema that = (CustomJsonRowDeserializationSchema) o;
            return Objects.equals(this.typeInfo, that.typeInfo) && Objects.equals(this.failOnMissingField, that.failOnMissingField);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.typeInfo, this.failOnMissingField});
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createConverter(TypeInformation<?> typeInfo) {
        CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter baseConverter = (CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter) this.createConverterForSimpleType(typeInfo).orElseGet(() -> {
            return (CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter) this.createContainerConverter(typeInfo).orElseGet(() -> {
                return this.createFallbackConverter(typeInfo.getTypeClass());
            });
        });
        return this.wrapIntoNullableConverter(baseConverter);
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter wrapIntoNullableConverter(CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter converter) {
        return (mapper, jsonNode) -> {
            return jsonNode.isNull() ? null : converter.convert(mapper, jsonNode);
        };
    }

    private Optional<CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter> createContainerConverter(TypeInformation<?> typeInfo) {
        if (typeInfo instanceof RowTypeInfo) {
            return Optional.of(this.createRowConverter((RowTypeInfo) typeInfo));
        } else if (typeInfo instanceof ObjectArrayTypeInfo) {
            return Optional.of(this.createObjectArrayConverter(((ObjectArrayTypeInfo) typeInfo).getComponentInfo()));
        } else if (typeInfo instanceof BasicArrayTypeInfo) {
            return Optional.of(this.createObjectArrayConverter(((BasicArrayTypeInfo) typeInfo).getComponentInfo()));
        } else if (this.isPrimitiveByteArray(typeInfo)) {
            return Optional.of(this.createByteArrayConverter());
        } else if (typeInfo instanceof MapTypeInfo) {
            MapTypeInfo<?, ?> mapTypeInfo = (MapTypeInfo) typeInfo;
            return Optional.of(this.createMapConverter(mapTypeInfo.getKeyTypeInfo(), mapTypeInfo.getValueTypeInfo()));
        } else {
            return Optional.empty();
        }
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createMapConverter(TypeInformation keyType, TypeInformation valueType) {
        CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter valueConverter = this.createConverter(valueType);
        CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter keyConverter = this.createConverter(keyType);
        return (mapper, jsonNode) -> {
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            HashMap result = new HashMap();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = (Map.Entry) fields.next();
                Object key = keyConverter.convert(mapper, TextNode.valueOf((String) entry.getKey()));
                Object value = valueConverter.convert(mapper, (JsonNode) entry.getValue());
                result.put(key, value);
            }

            return result;
        };
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createByteArrayConverter() {
        return (mapper, jsonNode) -> {
            try {
                return jsonNode.binaryValue();
            } catch (IOException var3) {
                throw new WrappingRuntimeException("Unable to deserialize byte array.", var3);
            }
        };
    }

    private boolean isPrimitiveByteArray(TypeInformation<?> typeInfo) {
        return typeInfo instanceof PrimitiveArrayTypeInfo && ((PrimitiveArrayTypeInfo) typeInfo).getComponentType() == Types.BYTE;
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createObjectArrayConverter(TypeInformation elementTypeInfo) {
        CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter elementConverter = this.createConverter(elementTypeInfo);
        return this.assembleArrayConverter(elementTypeInfo, elementConverter);
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createRowConverter(RowTypeInfo typeInfo) {
        List<CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter> fieldConverters = (List) Arrays.stream(typeInfo.getFieldTypes()).map(this::createConverter).collect(Collectors.toList());
        return this.assembleRowConverter(typeInfo.getFieldNames(), fieldConverters);
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter createFallbackConverter(Class<?> valueType) {
        return (mapper, jsonNode) -> {
            try {
                return mapper.treeToValue(jsonNode, valueType);
            } catch (JsonProcessingException var4) {
                throw new WrappingRuntimeException(String.format("Could not convert node: %s", jsonNode), var4);
            }
        };
    }

    private Optional<CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter> createConverterForSimpleType(TypeInformation<?> simpleTypeInfo) {
        if (simpleTypeInfo == Types.VOID) {
            return Optional.of((mapper, jsonNode) -> {
                return null;
            });
        } else if (simpleTypeInfo == Types.BOOLEAN) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.asBoolean();
            });
        } else if (simpleTypeInfo == Types.STRING) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.asText();
            });
        } else if (simpleTypeInfo == Types.INT) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.asInt();
            });
        } else if (simpleTypeInfo == Types.LONG) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.asLong();
            });
        } else if (simpleTypeInfo == Types.DOUBLE) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.asDouble();
            });
        } else if (simpleTypeInfo == Types.FLOAT) {
            return Optional.of((mapper, jsonNode) -> {
                return Float.parseFloat(jsonNode.asText().trim());
            });
        } else if (simpleTypeInfo == Types.SHORT) {
            return Optional.of((mapper, jsonNode) -> {
                return Short.parseShort(jsonNode.asText().trim());
            });
        } else if (simpleTypeInfo == Types.BYTE) {
            return Optional.of((mapper, jsonNode) -> {
                return Byte.parseByte(jsonNode.asText().trim());
            });
        } else if (simpleTypeInfo == Types.BIG_DEC) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.decimalValue();
            });
        } else if (simpleTypeInfo == Types.BIG_INT) {
            return Optional.of((mapper, jsonNode) -> {
                return jsonNode.bigIntegerValue();
            });
        } else if (simpleTypeInfo == Types.SQL_DATE) {
            return Optional.of(this::convertToDate);
        } else if (simpleTypeInfo == Types.SQL_TIME) {
            return Optional.of(this::convertToTime);
        } else if (simpleTypeInfo == Types.SQL_TIMESTAMP) {
            return Optional.of(this::convertToTimestamp);
        } else if (simpleTypeInfo == Types.LOCAL_DATE) {
            return Optional.of(this::convertToLocalDate);
        } else if (simpleTypeInfo == Types.LOCAL_TIME) {
            return Optional.of(this::convertToLocalTime);
        } else {
            return simpleTypeInfo == Types.LOCAL_DATE_TIME ? Optional.of(this::convertToLocalDateTime) : Optional.empty();
        }
    }

    private LocalDate convertToLocalDate(ObjectMapper mapper, JsonNode jsonNode) {
        return (LocalDate) DateTimeFormatter.ISO_LOCAL_DATE.parse(jsonNode.asText()).query(TemporalQueries.localDate());
    }

    private java.sql.Date convertToDate(ObjectMapper mapper, JsonNode jsonNode) {
        return Date.valueOf(this.convertToLocalDate(mapper, jsonNode));
    }

    private LocalDateTime convertToLocalDateTime(ObjectMapper mapper, JsonNode jsonNode) {
        TemporalAccessor parsedTimestamp = TimeFormats.RFC3339_TIMESTAMP_FORMAT.parse(jsonNode.asText());
        ZoneOffset zoneOffset = (ZoneOffset) parsedTimestamp.query(TemporalQueries.offset());
        if (zoneOffset != null && zoneOffset.getTotalSeconds() != 0) {
            throw new IllegalStateException("Invalid timestamp format. Only a timestamp in UTC timezone is supported yet. Format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        } else {
            LocalTime localTime = (LocalTime) parsedTimestamp.query(TemporalQueries.localTime());
            LocalDate localDate = (LocalDate) parsedTimestamp.query(TemporalQueries.localDate());
            return LocalDateTime.of(localDate, localTime);
        }
    }

    private Timestamp convertToTimestamp(ObjectMapper mapper, JsonNode jsonNode) {
        return Timestamp.valueOf(this.convertToLocalDateTime(mapper, jsonNode));
    }

    private LocalTime convertToLocalTime(ObjectMapper mapper, JsonNode jsonNode) {
        TemporalAccessor parsedTime = TimeFormats.RFC3339_TIME_FORMAT.parse(jsonNode.asText());
        ZoneOffset zoneOffset = (ZoneOffset) parsedTime.query(TemporalQueries.offset());
        LocalTime localTime = (LocalTime) parsedTime.query(TemporalQueries.localTime());
        if ((zoneOffset == null || zoneOffset.getTotalSeconds() == 0) && localTime.getNano() == 0) {
            return localTime;
        } else {
            throw new IllegalStateException("Invalid time format. Only a time in UTC timezone without milliseconds is supported yet.");
        }
    }

    private Time convertToTime(ObjectMapper mapper, JsonNode jsonNode) {
        return Time.valueOf(this.convertToLocalTime(mapper, jsonNode));
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter assembleRowConverter(String[] fieldNames, List<CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter> fieldConverters) {
        return (mapper, jsonNode) -> {
            ObjectNode node = (ObjectNode) jsonNode;
            int arity = fieldNames.length;
            Row row = new Row(arity);

            for (int i = 0; i < arity; ++i) {
                String fieldName = fieldNames[i];
                JsonNode field = node.get(fieldName);
                Object convertField = this.convertField(mapper, (CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter) fieldConverters.get(i), fieldName, field);
                row.setField(i, convertField);
            }

            return row;
        };
    }

    private Object convertField(ObjectMapper mapper, CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter fieldConverter, String fieldName, JsonNode field) {
        if (field == null) {
            if (this.failOnMissingField) {
                throw new IllegalStateException("Could not find field with name '" + fieldName + "'.");
            } else {
                return null;
            }
        } else {
            return fieldConverter.convert(mapper, field);
        }
    }

    private CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter assembleArrayConverter(TypeInformation<?> elementType, CustomJsonRowDeserializationSchema.DeserializationRuntimeConverter elementConverter) {
        Class<?> elementClass = elementType.getTypeClass();
        return (mapper, jsonNode) -> {
            ArrayNode node = (ArrayNode) jsonNode;
            Object[] array = (Object[]) ((Object[]) Array.newInstance(elementClass, node.size()));

            for (int i = 0; i < node.size(); ++i) {
                JsonNode innerNode = node.get(i);
                array[i] = elementConverter.convert(mapper, innerNode);
            }

            return array;
        };
    }

    @FunctionalInterface
    private interface DeserializationRuntimeConverter extends Serializable {
        Object convert(ObjectMapper var1, JsonNode var2);
    }

    public static class Builder {
        private final RowTypeInfo typeInfo;
        private boolean failOnMissingField;

        public Builder(TypeInformation<Row> typeInfo) {
            this.failOnMissingField = false;
            Preconditions.checkArgument(typeInfo instanceof RowTypeInfo, "Only RowTypeInfo is supported");
            this.typeInfo = (RowTypeInfo) typeInfo;
        }

        public Builder(String jsonSchema) {
            this(JsonRowSchemaConverter.convert((String) Preconditions.checkNotNull(jsonSchema)));
        }

        public CustomJsonRowDeserializationSchema.Builder failOnMissingField() {
            this.failOnMissingField = true;
            return this;
        }

        public CustomJsonRowDeserializationSchema build() {
            return new CustomJsonRowDeserializationSchema(this.typeInfo, this.failOnMissingField);
        }
    }
}
