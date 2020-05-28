package com.test.flink.sink

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericData, GenericRecord}
import org.apache.avro.reflect.ReflectData
import org.apache.avro.specific.{SpecificData, SpecificRecordBase}
import org.apache.flink.formats.parquet.{ParquetBuilder, ParquetWriterFactory}
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.column.ParquetProperties
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import org.apache.parquet.io.OutputFile

/**
  *
  * @author XiaShuai on 2020/5/22.
  */

object PaulParquetAvroWriters extends Serializable{
  private val serialVersionUID = 5757602721811399794L
  def forSpecificRecord[T <: SpecificRecordBase](`type`: Class[T], codecName: CompressionCodecName): ParquetWriterFactory[T] = {
    val schemaString = SpecificData.get.getSchema(`type`).toString
    val dataModel = SpecificData.get
    val builder = new ParquetBuilder[T] {
      override def createWriter(out: OutputFile): ParquetWriter[T] = {
        val schema = new Schema.Parser().parse(schemaString)
        AvroParquetWriter.builder[T](out).withSchema(schema).withDataModel(dataModel)
          .withCompressionCodec(codecName).withWriterVersion(ParquetProperties.WriterVersion.PARQUET_1_0)
          .build
      }
    }
    new ParquetWriterFactory(builder)
  }

  def forGenericRecord(schema: Schema, codecName: CompressionCodecName): ParquetWriterFactory[GenericRecord] = {
    val schemaString = schema.toString
    val dataModel = GenericData.get
    val builder = new ParquetBuilder[GenericRecord] {
      override def createWriter(out: OutputFile): ParquetWriter[GenericRecord] = {
        val schema = new Schema.Parser().parse(schemaString)
        AvroParquetWriter.builder[GenericRecord](out).withSchema(schema).withDataModel(dataModel)
          .withCompressionCodec(codecName).withWriterVersion(ParquetProperties.WriterVersion.PARQUET_1_0)
          .build
      }
    }
    new ParquetWriterFactory[GenericRecord](builder)
  }

  def forReflectRecord[T](`type`: Class[T], codecName: CompressionCodecName): ParquetWriterFactory[T] = {
    val schemaString = ReflectData.get.getSchema(`type`).toString
    val dataModel = ReflectData.get
    val builder = new ParquetBuilder[T] {
      override def createWriter(out: OutputFile): ParquetWriter[T] = {
        val schema = new Schema.Parser().parse(schemaString)
        AvroParquetWriter.builder[T](out).withSchema(schema).withDataModel(dataModel)
          .withCompressionCodec(codecName).withWriterVersion(ParquetProperties.WriterVersion.PARQUET_1_0)
          .build
      }
    }
    new ParquetWriterFactory[T](builder)
  }
}
