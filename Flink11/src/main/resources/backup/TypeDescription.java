//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.orc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DecimalColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.ListColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.MapColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.StructColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.UnionColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;

import static org.apache.orc.TypeDescription.parseCategory;

public class TypeDescription implements Comparable<TypeDescription>, Serializable {
    private static final int MAX_PRECISION = 38;
    private static final int MAX_SCALE = 38;
    private static final int DEFAULT_PRECISION = 38;
    private static final int DEFAULT_SCALE = 10;
    private static final int DEFAULT_LENGTH = 256;
    private final TypeDescription.Category category;
    private final List<TypeDescription> children;
    private final List<String> fieldNames;
    private int id = -1;
    private int maxId = -1;
    private TypeDescription parent;
    private int maxLength = 256;
    private int precision = 38;
    private int scale = 10;

    private TypeDescription(TypeDescription.Category category) {
        this.category = category;
        if (category.isPrimitive) {
            this.children = null;
        } else {
            this.children = new ArrayList();
        }

        if (category == TypeDescription.Category.STRUCT) {
            this.fieldNames = new ArrayList();
        } else {
            this.fieldNames = null;
        }

    }

    public static TypeDescription createBoolean() {
        return new TypeDescription(TypeDescription.Category.BOOLEAN);
    }

    public static TypeDescription createByte() {
        return new TypeDescription(TypeDescription.Category.BYTE);
    }

    public static TypeDescription createShort() {
        return new TypeDescription(TypeDescription.Category.SHORT);
    }

    public static TypeDescription createInt() {
        return new TypeDescription(TypeDescription.Category.INT);
    }

    public static TypeDescription createLong() {
        return new TypeDescription(TypeDescription.Category.LONG);
    }

    public static TypeDescription createFloat() {
        return new TypeDescription(TypeDescription.Category.FLOAT);
    }

    public static TypeDescription createDouble() {
        return new TypeDescription(TypeDescription.Category.DOUBLE);
    }

    public static TypeDescription createString() {
        return new TypeDescription(TypeDescription.Category.STRING);
    }

    public static TypeDescription createDate() {
        return new TypeDescription(TypeDescription.Category.DATE);
    }

    public static TypeDescription createTimestamp() {
        return new TypeDescription(TypeDescription.Category.TIMESTAMP);
    }

    public static TypeDescription createBinary() {
        return new TypeDescription(TypeDescription.Category.BINARY);
    }

    public static TypeDescription createDecimal() {
        return new TypeDescription(TypeDescription.Category.DECIMAL);
    }

    static TypeDescription.Category parseCategory(TypeDescription.StringPosition source) {
        int start;
        for (start = source.position; source.position < source.length; ++source.position) {
            char ch = source.value.charAt(source.position);
            if (!Character.isLetter(ch)) {
                break;
            }
        }

        if (source.position != start) {
            String word = source.value.substring(start, source.position).toLowerCase();
            TypeDescription.Category[] var3 = TypeDescription.Category.values();
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                TypeDescription.Category cat = var3[var5];
                if (cat.getName().equals(word)) {
                    return cat;
                }
            }
        }

        throw new IllegalArgumentException("Can't parse category at " + source);
    }

    static int parseInt(TypeDescription.StringPosition source) {
        int start = source.position;

        int result;
        for (result = 0; source.position < source.length; ++source.position) {
            char ch = source.value.charAt(source.position);
            if (!Character.isDigit(ch)) {
                break;
            }

            result = result * 10 + (ch - 48);
        }

        if (source.position == start) {
            throw new IllegalArgumentException("Missing integer at " + source);
        } else {
            return result;
        }
    }

    static void parseUnion(TypeDescription type, TypeDescription.StringPosition source) {
        requireChar(source, '<');

        do {
            type.addUnionChild(parseType(source));
        } while (consumeChar(source, ','));

        requireChar(source, '>');
    }

    static void parseStruct(TypeDescription type, TypeDescription.StringPosition source) {
        requireChar(source, '<');
        boolean needComma = false;

        while (!consumeChar(source, '>')) {
            if (needComma) {
                requireChar(source, ',');
            } else {
                needComma = true;
            }

            String fieldName = parseName(source);
            requireChar(source, ':');
            type.addField(fieldName, parseType(source));
        }

    }

    static String parseName(TypeDescription.StringPosition source) {
        if (source.position == source.length) {
            throw new IllegalArgumentException("Missing name at " + source);
        } else {
            int start = source.position;
            if (source.value.charAt(source.position) == '`') {
                ++source.position;
                StringBuilder buffer = new StringBuilder();
                boolean closed = false;

                while (source.position < source.length) {
                    char ch = source.value.charAt(source.position);
                    ++source.position;
                    if (ch == '`') {
                        if (source.position >= source.length || source.value.charAt(source.position) != '`') {
                            closed = true;
                            break;
                        }

                        ++source.position;
                        buffer.append('`');
                    } else {
                        buffer.append(ch);
                    }
                }

                if (!closed) {
                    source.position = start;
                    throw new IllegalArgumentException("Unmatched quote at " + source);
                } else if (buffer.length() == 0) {
                    throw new IllegalArgumentException("Empty quoted field name at " + source);
                } else {
                    return buffer.toString();
                }
            } else {
                while (source.position < source.length) {
                    char ch = source.value.charAt(source.position);
                    if (!Character.isLetterOrDigit(ch) && ch != '_') {
                        break;
                    }

                    ++source.position;
                }

                if (source.position == start) {
                    throw new IllegalArgumentException("Missing name at " + source);
                } else {
                    return source.value.substring(start, source.position);
                }
            }
        }
    }

    static boolean consumeChar(TypeDescription.StringPosition source, char ch) {
        boolean result = source.position < source.length && source.value.charAt(source.position) == ch;
        if (result) {
            ++source.position;
        }

        return result;
    }

    static void requireChar(TypeDescription.StringPosition source, char required) {
        if (source.position < source.length && source.value.charAt(source.position) == required) {
            ++source.position;
        } else {
            throw new IllegalArgumentException("Missing required char '" + required + "' at " + source);
        }
    }

    static TypeDescription parseType(TypeDescription.StringPosition source) {
        TypeDescription result = new TypeDescription(parseCategory(source));
        TypeDescription keyType;
        switch (result.getCategory()) {
            case CHAR:
            case VARCHAR:
                requireChar(source, '(');
                result.withMaxLength(parseInt(source));
                requireChar(source, ')');
                break;
            case DECIMAL:
                requireChar(source, '(');
                int precision = parseInt(source);
                requireChar(source, ',');
                result.withScale(parseInt(source));
                result.withPrecision(precision);
                requireChar(source, ')');
                break;
            case UNION:
                parseUnion(result, source);
                break;
            case LIST:
                requireChar(source, '<');
                keyType = parseType(source);
                result.children.add(keyType);
                keyType.parent = result;
                requireChar(source, '>');
                break;
            case MAP:
                requireChar(source, '<');
                keyType = parseType(source);
                result.children.add(keyType);
                keyType.parent = result;
                requireChar(source, ',');
                TypeDescription valueType = parseType(source);
                result.children.add(valueType);
                valueType.parent = result;
                requireChar(source, '>');
                break;
            case STRUCT:
                parseStruct(result, source);
            case BINARY:
            case BOOLEAN:
            case BYTE:
            case DATE:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
            case STRING:
            case TIMESTAMP:
                break;
            default:
                throw new IllegalArgumentException("Unknown type " + result.getCategory() + " at " + source);
        }

        return result;
    }

    public static TypeDescription fromString(String typeName) {
        if (typeName == null) {
            return null;
        } else {
            TypeDescription.StringPosition source = new TypeDescription.StringPosition(typeName);
            TypeDescription result = parseType(source);
            if (source.position != source.length) {
                throw new IllegalArgumentException("Extra characters at " + source);
            } else {
                return result;
            }
        }
    }

    public static TypeDescription createVarchar() {
        return new TypeDescription(TypeDescription.Category.VARCHAR);
    }

    public static TypeDescription createChar() {
        return new TypeDescription(TypeDescription.Category.CHAR);
    }

    public static TypeDescription createList(TypeDescription childType) {
        TypeDescription result = new TypeDescription(TypeDescription.Category.LIST);
        result.children.add(childType);
        childType.parent = result;
        return result;
    }

    public static TypeDescription createMap(TypeDescription keyType, TypeDescription valueType) {
        TypeDescription result = new TypeDescription(TypeDescription.Category.MAP);
        result.children.add(keyType);
        result.children.add(valueType);
        keyType.parent = result;
        valueType.parent = result;
        return result;
    }

    public static TypeDescription createUnion() {
        return new TypeDescription(TypeDescription.Category.UNION);
    }

    public static TypeDescription createStruct() {
        return new TypeDescription(TypeDescription.Category.STRUCT);
    }

    public int compareTo(TypeDescription other) {
        if (this == other) {
            return 0;
        } else if (other == null) {
            return -1;
        } else {
            int result = this.category.compareTo(other.category);
            if (result == 0) {
                int c;
                switch (this.category) {
                    case CHAR:
                    case VARCHAR:
                        return this.maxLength - other.maxLength;
                    case DECIMAL:
                        if (this.precision != other.precision) {
                            return this.precision - other.precision;
                        }

                        return this.scale - other.scale;
                    case UNION:
                    case LIST:
                    case MAP:
                        if (this.children.size() != other.children.size()) {
                            return this.children.size() - other.children.size();
                        }

                        for (c = 0; result == 0 && c < this.children.size(); ++c) {
                            result = ((TypeDescription) this.children.get(c)).compareTo((TypeDescription) other.children.get(c));
                        }

                        return result;
                    case STRUCT:
                        if (this.children.size() != other.children.size()) {
                            return this.children.size() - other.children.size();
                        }

                        for (c = 0; result == 0 && c < this.children.size(); ++c) {
                            result = ((String) this.fieldNames.get(c)).compareTo((String) other.fieldNames.get(c));
                            if (result == 0) {
                                result = ((TypeDescription) this.children.get(c)).compareTo((TypeDescription) other.children.get(c));
                            }
                        }
                }
            }

            return result;
        }
    }

    public TypeDescription withPrecision(int precision) {
        if (this.category != TypeDescription.Category.DECIMAL) {
            throw new IllegalArgumentException("precision is only allowed on decimal and not " + this.category.name);
        } else if (precision >= 1 && precision <= 38 && this.scale <= precision) {
            this.precision = precision;
            return this;
        } else {
            throw new IllegalArgumentException("precision " + precision + " is out of range 1 .. " + this.scale);
        }
    }

    public TypeDescription withScale(int scale) {
        if (this.category != TypeDescription.Category.DECIMAL) {
            throw new IllegalArgumentException("scale is only allowed on decimal and not " + this.category.name);
        } else if (scale >= 0 && scale <= 38 && scale <= this.precision) {
            this.scale = scale;
            return this;
        } else {
            throw new IllegalArgumentException("scale is out of range at " + scale);
        }
    }

    public TypeDescription withMaxLength(int maxLength) {
        if (this.category != TypeDescription.Category.VARCHAR && this.category != TypeDescription.Category.CHAR) {
            throw new IllegalArgumentException("maxLength is only allowed on char and varchar and not " + this.category.name);
        } else {
            this.maxLength = maxLength;
            return this;
        }
    }

    public TypeDescription addUnionChild(TypeDescription child) {
        if (this.category != TypeDescription.Category.UNION) {
            throw new IllegalArgumentException("Can only add types to union type and not " + this.category);
        } else {
            this.children.add(child);
            child.parent = this;
            return this;
        }
    }

    public TypeDescription addField(String field, TypeDescription fieldType) {
        if (this.category != TypeDescription.Category.STRUCT) {
            throw new IllegalArgumentException("Can only add fields to struct type and not " + this.category);
        } else {
            this.fieldNames.add(field);
            this.children.add(fieldType);
            fieldType.parent = this;
            return this;
        }
    }

    public int getId() {
        if (this.id == -1) {
            TypeDescription root;
            for (root = this; root.parent != null; root = root.parent) {
            }

            root.assignIds(0);
        }

        return this.id;
    }

    public TypeDescription clone() {
        TypeDescription result = new TypeDescription(this.category);
        result.maxLength = this.maxLength;
        result.precision = this.precision;
        result.scale = this.scale;
        if (this.fieldNames != null) {
            result.fieldNames.addAll(this.fieldNames);
        }

        if (this.children != null) {
            Iterator var2 = this.children.iterator();

            while (var2.hasNext()) {
                TypeDescription child = (TypeDescription) var2.next();
                TypeDescription clone = child.clone();
                clone.parent = result;
                result.children.add(clone);
            }
        }

        return result;
    }

    public int hashCode() {
        long result = (long) (this.category.ordinal() * 4241 + this.maxLength + this.precision * 13 + this.scale);
        TypeDescription child;
        if (this.children != null) {
            for (Iterator var3 = this.children.iterator(); var3.hasNext(); result = result * 6959L + (long) child.hashCode()) {
                child = (TypeDescription) var3.next();
            }
        }

        return (int) result;
    }

    public boolean equals(Object other) {
        if (other != null && other instanceof TypeDescription) {
            if (other == this) {
                return true;
            } else {
                TypeDescription castOther = (TypeDescription) other;
                if (this.category == castOther.category && this.maxLength == castOther.maxLength && this.scale == castOther.scale && this.precision == castOther.precision) {
                    int i;
                    if (this.children != null) {
                        if (this.children.size() != castOther.children.size()) {
                            return false;
                        }

                        for (i = 0; i < this.children.size(); ++i) {
                            if (!((TypeDescription) this.children.get(i)).equals(castOther.children.get(i))) {
                                return false;
                            }
                        }
                    }

                    if (this.category == TypeDescription.Category.STRUCT) {
                        for (i = 0; i < this.fieldNames.size(); ++i) {
                            if (!((String) this.fieldNames.get(i)).equals(castOther.fieldNames.get(i))) {
                                return false;
                            }
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public int getMaximumId() {
        if (this.maxId == -1) {
            TypeDescription root;
            for (root = this; root.parent != null; root = root.parent) {
            }

            root.assignIds(0);
        }

        return this.maxId;
    }

    private ColumnVector createColumn(int maxSize) {
        ColumnVector[] fieldVector;
        int i;
        switch (this.category) {
            case CHAR:
            case VARCHAR:
            case STRING:
            case BINARY:
                return new BytesColumnVector(maxSize);
            case DECIMAL:
                return new DecimalColumnVector(maxSize, this.precision, this.scale);
            case UNION:
                fieldVector = new ColumnVector[this.children.size()];

                for (i = 0; i < fieldVector.length; ++i) {
                    fieldVector[i] = ((TypeDescription) this.children.get(i)).createColumn(maxSize);
                }

                return new UnionColumnVector(maxSize, fieldVector);
            case LIST:
                return new ListColumnVector(maxSize, ((TypeDescription) this.children.get(0)).createColumn(maxSize));
            case MAP:
                return new MapColumnVector(maxSize, ((TypeDescription) this.children.get(0)).createColumn(maxSize), ((TypeDescription) this.children.get(1)).createColumn(maxSize));
            case STRUCT:
                fieldVector = new ColumnVector[this.children.size()];

                for (i = 0; i < fieldVector.length; ++i) {
                    fieldVector[i] = ((TypeDescription) this.children.get(i)).createColumn(maxSize);
                }

                return new StructColumnVector(maxSize, fieldVector);
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case DATE:
                return new LongColumnVector(maxSize);
            case TIMESTAMP:
                return new TimestampColumnVector(maxSize);
            case FLOAT:
            case DOUBLE:
                return new DoubleColumnVector(maxSize);
            default:
                throw new IllegalArgumentException("Unknown type " + this.category);
        }
    }

    public VectorizedRowBatch createRowBatch(int maxSize) {
        VectorizedRowBatch result;
        if (this.category == TypeDescription.Category.STRUCT) {
            result = new VectorizedRowBatch(this.children.size(), maxSize);

            for (int i = 0; i < result.cols.length; ++i) {
                result.cols[i] = ((TypeDescription) this.children.get(i)).createColumn(maxSize);
            }
        } else {
            result = new VectorizedRowBatch(1, maxSize);
            result.cols[0] = this.createColumn(maxSize);
        }

        result.reset();
        return result;
    }

    public VectorizedRowBatch createRowBatch() {
        return this.createRowBatch(1024);
    }

    public TypeDescription.Category getCategory() {
        return this.category;
    }

    public int getMaxLength() {
        return this.maxLength;
    }

    public int getPrecision() {
        return this.precision;
    }

    public int getScale() {
        return this.scale;
    }

    public List<String> getFieldNames() {
        return Collections.unmodifiableList(this.fieldNames);
    }

    public List<TypeDescription> getChildren() {
        return this.children == null ? null : Collections.unmodifiableList(this.children);
    }

    private int assignIds(int startId) {
        this.id = startId++;
        TypeDescription child;
        if (this.children != null) {
            for (Iterator var2 = this.children.iterator(); var2.hasNext(); startId = child.assignIds(startId)) {
                child = (TypeDescription) var2.next();
            }
        }

        this.maxId = startId - 1;
        return startId;
    }

    public void printToBuffer(StringBuilder buffer) {
        buffer.append(this.category.name);
        int i;
        switch (this.category) {
            case CHAR:
            case VARCHAR:
                buffer.append('(');
                buffer.append(this.maxLength);
                buffer.append(')');
                break;
            case DECIMAL:
                buffer.append('(');
                buffer.append(this.precision);
                buffer.append(',');
                buffer.append(this.scale);
                buffer.append(')');
                break;
            case UNION:
            case LIST:
            case MAP:
                buffer.append('<');

                for (i = 0; i < this.children.size(); ++i) {
                    if (i != 0) {
                        buffer.append(',');
                    }

                    ((TypeDescription) this.children.get(i)).printToBuffer(buffer);
                }

                buffer.append('>');
                break;
            case STRUCT:
                buffer.append('<');

                for (i = 0; i < this.children.size(); ++i) {
                    if (i != 0) {
                        buffer.append(',');
                    }

                    buffer.append((String) this.fieldNames.get(i));
                    buffer.append(':');
                    ((TypeDescription) this.children.get(i)).printToBuffer(buffer);
                }

                buffer.append('>');
        }

    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        this.printToBuffer(buffer);
        return buffer.toString();
    }

    private void printJsonToBuffer(String prefix, StringBuilder buffer, int indent) {
        int i;
        for (i = 0; i < indent; ++i) {
            buffer.append(' ');
        }

        buffer.append(prefix);
        buffer.append("{\"category\": \"");
        buffer.append(this.category.name);
        buffer.append("\", \"id\": ");
        buffer.append(this.getId());
        buffer.append(", \"max\": ");
        buffer.append(this.maxId);
        switch (this.category) {
            case CHAR:
            case VARCHAR:
                buffer.append(", \"length\": ");
                buffer.append(this.maxLength);
                break;
            case DECIMAL:
                buffer.append(", \"precision\": ");
                buffer.append(this.precision);
                buffer.append(", \"scale\": ");
                buffer.append(this.scale);
                break;
            case UNION:
            case LIST:
            case MAP:
                buffer.append(", \"children\": [");

                for (i = 0; i < this.children.size(); ++i) {
                    buffer.append('\n');
                    ((TypeDescription) this.children.get(i)).printJsonToBuffer("", buffer, indent + 2);
                    if (i != this.children.size() - 1) {
                        buffer.append(',');
                    }
                }

                buffer.append("]");
                break;
            case STRUCT:
                buffer.append(", \"fields\": [");

                for (i = 0; i < this.children.size(); ++i) {
                    buffer.append('\n');
                    ((TypeDescription) this.children.get(i)).printJsonToBuffer("\"" + (String) this.fieldNames.get(i) + "\": ", buffer, indent + 2);
                    if (i != this.children.size() - 1) {
                        buffer.append(',');
                    }
                }

                buffer.append(']');
        }

        buffer.append('}');
    }

    public String toJson() {
        StringBuilder buffer = new StringBuilder();
        this.printJsonToBuffer("", buffer, 0);
        return buffer.toString();
    }

    public static enum Category {
        BOOLEAN("boolean", true),
        BYTE("tinyint", true),
        SHORT("smallint", true),
        INT("int", true),
        LONG("bigint", true),
        FLOAT("float", true),
        DOUBLE("double", true),
        STRING("string", true),
        DATE("date", true),
        TIMESTAMP("timestamp", true),
        BINARY("binary", true),
        DECIMAL("decimal", true),
        VARCHAR("varchar", true),
        CHAR("char", true),
        LIST("array", false),
        MAP("map", false),
        STRUCT("struct", false),
        UNION("uniontype", false);

        final boolean isPrimitive;
        final String name;

        private Category(String name, boolean isPrimitive) {
            this.name = name;
            this.isPrimitive = isPrimitive;
        }

        public boolean isPrimitive() {
            return this.isPrimitive;
        }

        public String getName() {
            return this.name;
        }
    }

    static class StringPosition {
        final String value;
        final int length;
        int position;

        StringPosition(String value) {
            this.value = value;
            this.position = 0;
            this.length = value.length();
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder();
            buffer.append('\'');
            buffer.append(this.value.substring(0, this.position));
            buffer.append('^');
            buffer.append(this.value.substring(this.position));
            buffer.append('\'');
            return buffer.toString();
        }
    }
}
