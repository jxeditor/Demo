package com.test.spark.log

import org.apache.log4j.{Level, Logger}
import org.apache.spark.internal.Logging

object LoggerLevels extends Logging {

  def setLogLevels() {
    val log4jInitialized = Logger.getRootLogger.getAllAppenders.hasMoreElements
    if (!log4jInitialized) {
      logInfo("Setting com.dev.spark.log level to [WARN] for streaming example." +
        " To override add a custom log4j.properties to the classpath.")
      Logger.getRootLogger.setLevel(Level.ERROR)
    }
  }
}