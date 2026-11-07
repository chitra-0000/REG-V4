package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.vo.FilteringReferentials;

import java.util.List;

public class ExportWatchlistAmeFilteringData {
    private final List<RecordFilteringData> recordFilteringData;
    private final FilteringReferentials filteringReferentials;

    public ExportWatchlistAmeFilteringData(List<RecordFilteringData> recordFilteringData, FilteringReferentials filteringReferentials) {
        this.recordFilteringData = recordFilteringData;
        this.filteringReferentials = filteringReferentials;
    }

    public List<RecordFilteringData> getRecordFilteringData() { return recordFilteringData; }

    public FilteringReferentials getFilteringReferentials() { return filteringReferentials; }
}

