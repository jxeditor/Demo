package org.apache.flink.formats.custom;

import com.test.flink.CustomJsonRowDeserializationSchema;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.formats.json.JsonRowSchemaConverter;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.descriptors.JsonValidator;
import org.apache.flink.table.factories.DeserializationSchemaFactory;
import org.apache.flink.table.factories.SerializationSchemaFactory;
import org.apache.flink.table.factories.TableFormatFactoryBase;
import org.apache.flink.types.Row;

import java.util.Map;

/**
 * @author XiaShuai on 2020/6/11.
 */
public class CustomJsonRowFormatFactory extends TableFormatFactoryBase<Row>
        implements SerializationSchemaFactory<Row>, DeserializationSchemaFactory<Row> {

    public CustomJsonRowFormatFactory() {
        super("custom", 1, true);
    }

    private static DescriptorProperties getValidatedProperties(Map<String, String> propertiesMap) {
        final DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(propertiesMap);

        // validate
        new JsonValidator().validate(descriptorProperties);

        return descriptorProperties;
    }

    @Override
    public DeserializationSchema<Row> createDeserializationSchema(Map<String, String> properties) {
        final DescriptorProperties descriptorProperties = getValidatedProperties(properties);

        // create and configure
        final CustomJsonRowDeserializationSchema.Builder schema =
                new CustomJsonRowDeserializationSchema.Builder(createTypeInformation(descriptorProperties));

        return schema.build();
    }

    @Override
    public SerializationSchema<Row> createSerializationSchema(Map<String, String> properties) {
        final DescriptorProperties descriptorProperties = getValidatedProperties(properties);
        return new JsonRowSerializationSchema.Builder(createTypeInformation(descriptorProperties)).build();
    }

    private TypeInformation<Row> createTypeInformation(DescriptorProperties descriptorProperties) {
        if (descriptorProperties.containsKey(JsonValidator.FORMAT_SCHEMA)) {
            return (RowTypeInfo) descriptorProperties.getType(JsonValidator.FORMAT_SCHEMA);
        } else if (descriptorProperties.containsKey(JsonValidator.FORMAT_JSON_SCHEMA)) {
            return JsonRowSchemaConverter.convert(descriptorProperties.getString(JsonValidator.FORMAT_JSON_SCHEMA));
        } else {
            return deriveSchema(descriptorProperties.asMap()).toRowType();
        }
    }
}
