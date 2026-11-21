package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.six.entity.FilteredInstrumentFile;
import com.bnpp.regliss.entity.six.entity.FilteredStructuredFile;

import java.util.*;

public final class SixIsinMergeContent {

    private static final SixIsinMergeContent NONE = new SixIsinMergeContent(Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap());

    private final Set<String> activeInstrumentIsins;
    private final Map<String, List<FilteredInstrumentFile>> instrumentRows;
    private final Map<String, List<FilteredStructuredFile>> structuredRows;

    public SixIsinMergeContent(Set<String> activeInstrumentIsins, Map<String, List<FilteredInstrumentFile>> instrumentRows,
                               Map<String, List<FilteredStructuredFile>> structuredRows) {
        this.activeInstrumentIsins = activeInstrumentIsins == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(activeInstrumentIsins));
        this.instrumentRows = instrumentRows == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(instrumentRows));
        this.structuredRows = structuredRows == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(structuredRows));
    }

    public static SixIsinMergeContent none() {
        return NONE;
    }

    public boolean hasActiveInstrument(String isin) {
        return isin != null && activeInstrumentIsins.contains(isin);
    }

    public List<FilteredInstrumentFile> instrumentRows(String isin) {
        List<FilteredInstrumentFile> rows = isin == null ? null : instrumentRows.get(isin);
        return rows == null ? Collections.emptyList() : Collections.unmodifiableList(rows);
    }

    public List<FilteredStructuredFile> structuredRows(String hostIsin) {
        List<FilteredStructuredFile> rows = hostIsin == null ? null : structuredRows.get(hostIsin);
        return rows == null ? Collections.emptyList() : Collections.unmodifiableList(rows);
    }

    public int activeInstrumentCount() {
        return activeInstrumentIsins.size();
    }

    public int mergedIsinCount() {
        Set<String> isins = new HashSet<>(instrumentRows.keySet());
        isins.addAll(structuredRows.keySet());
        return isins.size();
    }
}

