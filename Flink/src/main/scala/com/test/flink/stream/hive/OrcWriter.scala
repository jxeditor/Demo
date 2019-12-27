package com.test.flink.stream.hive

import org.apache.flink.streaming.connectors.fs.Writer
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.hive.ql.io.orc.OrcFile
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory.ObjectInspectorOptions
import org.apache.hadoop.hive.serde2.objectinspector.{ObjectInspector, ObjectInspectorFactory}
import org.apache.flink.api.scala._
import scala.util.Random

class OrcWriter[T](struct: Class[T]) extends Writer[T] with Serializable {

  @transient var writer: org.apache.hadoop.hive.ql.io.orc.Writer = null
  @transient var inspector: ObjectInspector = null
  @transient var basePath: Path = null
  @transient var fileSystem: FileSystem = null

  override def duplicate() = new OrcWriter(struct)

  override def open(fs: FileSystem, path: Path) = {
    basePath = path
    fileSystem = fs
    inspector = ObjectInspectorFactory.getReflectionObjectInspector(struct, ObjectInspectorOptions.JAVA)
    initWriter
  }

  private def initWriter(): Unit = {
    val newPath = getNewPath()
    writer = OrcFile.createWriter(newPath, OrcFile.writerOptions(fileSystem.getConf).inspector(inspector))
  }

  override def write(element: T) = {
    if (writer == null)
      initWriter()
    writer.addRow(element)
  }

  override def flush() = {
    if (writer == null)
      throw new IllegalStateException("Writer is not open")
    val before = writer.getRawDataSize
    writer.writeIntermediateFooter()
    val after = writer.getRawDataSize
    println(s"###################################$before ==> $after###################################")
    writer.getRawDataSize
  }

  override def getPos = flush()

  override def close() = {
    if (writer != null) writer.close()
  }

  private def getNewPath(): Path = {
    var newPath: Path = null
    synchronized {
      newPath = new Path(basePath.getParent, getRandomPartName)
      while (fileSystem.exists(newPath)) {
        newPath = new Path(basePath.getParent, getRandomPartName)
      }
    }
    newPath
  }

  private def getRandomPartName(): String = {
    val suffix = math.abs(Random.nextLong())
    s"part_${suffix}.orc"
  }
}
