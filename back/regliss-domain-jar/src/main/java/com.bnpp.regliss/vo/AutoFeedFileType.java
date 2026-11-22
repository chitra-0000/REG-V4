package com.bnpp.regliss.vo;

public enum AutoFeedFileType {
    XML_DATA_FILE,
    XML_CHECKSUM_FILE,
    XML_LOG_FILE,
    CSV_DATA_FILE,
    CSV_CHECKSUM_FILE,
    CSV_LOG_FILE;

    public boolean isXml() {
        return this == XML_DATA_FILE || this == XML_CHECKSUM_FILE || this == XML_LOG_FILE;
    }

    public boolean isCsv() {
        return this == CSV_DATA_FILE || this == CSV_CHECKSUM_FILE || this == CSV_LOG_FILE;
    }

    public boolean isFeedLog() {
        return this == XML_CHECKSUM_FILE || this == XML_LOG_FILE || this == CSV_CHECKSUM_FILE || this == CSV_LOG_FILE;
    }
}
