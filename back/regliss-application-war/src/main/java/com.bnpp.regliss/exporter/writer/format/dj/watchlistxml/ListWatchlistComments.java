package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ListWatchlistComments {

    DJ_LOG_RECORDS("number of records from DJ Logs"),
    EXCLUDED_RECORDS("number of records excluded by the filters"),
    FILTERED_RECORDS("number of records exported after application of the filters"),
    DATABASE_RECORDS("number of records in the current version");
    @Getter
    private String comment;
}
