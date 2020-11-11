/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.data.conversion;

import org.apache.flink.annotation.Internal;
import org.apache.flink.table.data.DecimalDataUtils;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.binary.BinaryArrayData;
import org.apache.flink.table.types.AtomicDataType;
import org.apache.flink.table.types.CollectionDataType;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.FieldsDataType;
import org.apache.flink.table.types.logical.*;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Converter for {@link RowType} of {@link Row} external type.
 */
@Internal
public class RowRowConverter implements DataStructureConverter<RowData, Row> {

	private static final long serialVersionUID = 1L;

	private final DataStructureConverter<Object, Object>[] fieldConverters;

	private final RowData.FieldGetter[] fieldGetters;

	private RowRowConverter(
			DataStructureConverter<Object, Object>[] fieldConverters,
			RowData.FieldGetter[] fieldGetters) {
		this.fieldConverters = fieldConverters;
		this.fieldGetters = fieldGetters;
	}

	@Override
	public void open(ClassLoader classLoader) {
		for (DataStructureConverter<Object, Object> fieldConverter : fieldConverters) {
			fieldConverter.open(classLoader);
		}
	}

	@Override
	public RowData toInternal(Row external) {
		final int length = fieldConverters.length;
		final GenericRowData genericRow = new GenericRowData(external.getKind(), length);
		for (int pos = 0; pos < length; pos++) {
			final Object value = external.getField(pos);
			genericRow.setField(pos, fieldConverters[pos].toInternalOrNull(value));
		}
		return genericRow;
	}

	@Override
	public Row toExternal(RowData internal) {
		final int length = fieldConverters.length;
//		BinaryArrayData binaryArrayData = BinaryArrayData.fromPrimitiveArray(new double[]{0.04, 0.05, 0.06});
//
//		binaryArrayData.setDecimal(0, DecimalDataUtils.castFrom("0.1", 2, 1),2);
//		binaryArrayData.setDecimal(1, DecimalDataUtils.castFrom("0.02", 3, 2),3);
//		binaryArrayData.setDecimal(2, DecimalDataUtils.castFrom("0.03", 3, 2),3);
//
//		GenericRowData genericRowData = new GenericRowData(RowKind.INSERT, 1);
//		genericRowData.setField(0,binaryArrayData);
//
//		System.out.println(genericRowData);

		final Row row = new Row(internal.getRowKind(), length);
		for (int pos = 0; pos < length; pos++) {
			final Object value = fieldGetters[pos].getFieldOrNull(internal);
			fieldConverters[pos].toExternalOrNull(value);
			row.setField(pos, fieldConverters[pos].toExternalOrNull(value));
		}
		return row;
	}

	// --------------------------------------------------------------------------------------------
	// Factory method
	// --------------------------------------------------------------------------------------------

	@SuppressWarnings({"unchecked", "Convert2MethodRef"})
	public static RowRowConverter create(DataType dataType) {
		final List<DataType> fields = dataType.getChildren();
		final DataStructureConverter<Object, Object>[] fieldConverters = fields.stream()
			.map(dt -> DataStructureConverters.getConverter(dt))
			.toArray(DataStructureConverter[]::new);
		final RowData.FieldGetter[] fieldGetters = IntStream
			.range(0, fields.size())
			.mapToObj(pos -> RowData.createFieldGetter(fields.get(pos).getLogicalType(), pos))
			.toArray(RowData.FieldGetter[]::new);
		return new RowRowConverter(fieldConverters, fieldGetters);
	}
}
