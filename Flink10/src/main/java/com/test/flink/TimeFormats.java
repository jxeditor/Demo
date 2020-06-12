package com.test.flink;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

class TimeFormats {
    static final DateTimeFormatter RFC3339_TIME_FORMAT;
    static final DateTimeFormatter RFC3339_TIMESTAMP_FORMAT;

    private TimeFormats() {
    }

    static {
        RFC3339_TIME_FORMAT = (new DateTimeFormatterBuilder()).appendPattern("HH:mm:ss").appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).appendPattern("'Z'").toFormatter();
        RFC3339_TIMESTAMP_FORMAT = (new DateTimeFormatterBuilder()).append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').append(RFC3339_TIME_FORMAT).toFormatter();
    }
}
