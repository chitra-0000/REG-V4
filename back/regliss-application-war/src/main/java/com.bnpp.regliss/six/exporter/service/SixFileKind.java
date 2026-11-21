package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.ImportFileType;

import java.util.Arrays;
import java.util.Optional;

public enum SixFileKind {

    INSTRUMENT(ImportFileType.SIX_INSTRUMENTS_FILE,
            new Names("INSTR", "Instrument File", "INSTR"),
            new Tables("SIX_INSTRUMENTS", null, "i.isin", "SIX_INSTRU_ID",
                    "FILTERED_SIX_INSTRUMENTS", "IDX_SIX_INSTRUMENT_VERSION", "IDX_SIX_TARGET_INSTR_ID")),

    STRUCTURED(ImportFileType.SIX_STRUCTURED_FILE,
            new Names("STRUCT", "Structured File", "STRUCT"),
            new Tables("SIX_STRUCTURED", "UNDERLYING_CH", "i.host_isin", "SIX_STRUCT_ID",
                    "FILTERED_SIX_STRUCTURED", "IDX_SIX_STRUCTURE_VERSION", "IDX_SIX_TARGET_STRUCT_ID")),

    OPTIONS(ImportFileType.SIX_OPTIONS_FILE,
            new Names("OPT", "Options File", "OPTIONS"),
            new Tables("SIX_OPTION", "UNDERLYING_CH", "i.isin_option", "SIX_OPT_ID",
                    "FILTERED_SIX_OPTION", "IDX_SIX_OPT_VERSION", "IDX_SIX_TARGET_OPT_ID"));

    private final ImportFileType importFileType;
    private final String fileNameMarker;
    private final String table;
    private final String instrumentLinkColumn;
    private final String filterFileType;
    private final String pollerFileType;
    private final String rootIsinColumn;
    private final String targetFkColumn;
    private final String filteredTable;
    private final String versionIndex;
    private final String targetFkIndex;

    SixFileKind(ImportFileType importFileType, Names names, Tables tables) {
        this.importFileType = importFileType;
        this.fileNameMarker = names.fileNameMarker;
        this.filterFileType = names.filterFileType;
        this.pollerFileType = names.pollerFileType;
        this.table = tables.table;
        this.instrumentLinkColumn = tables.instrumentLinkColumn;
        this.rootIsinColumn = tables.rootIsinColumn;
        this.targetFkColumn = tables.targetFkColumn;
        this.filteredTable = tables.filteredTable;
        this.versionIndex = tables.versionIndex;
        this.targetFkIndex = tables.targetFkIndex;
    }

    public static SixFileKind source() {
        return INSTRUMENT;
    }

    public boolean isSource() {
        return this == source();
    }

    public boolean receivesConfidence() {
        return instrumentLinkColumn != null;
    }

    public ImportFileType getImportFileType() { return importFileType; }

    public String getTable() { return table; }

    public String getInstrumentLinkColumn() { return instrumentLinkColumn; }

    public String getFilterFileType() { return filterFileType; }

    public String getPollerFileType() { return pollerFileType; }

    public String getRootIsinColumn() { return rootIsinColumn; }

    public String getTargetFkColumn() { return targetFkColumn; }

    public String getFilteredTable() { return filteredTable; }

    public String getVersionIndex() { return versionIndex; }

    public String getTargetFkIndex() { return targetFkIndex; }

    public String joinHint() {
        return "/*+ LEADING(i) USE_NL(t) INDEX(i " + versionIndex + ") INDEX(t " + targetFkIndex + ") */";
    }

    public String joinHintWithExclusion() {
        return "/*+ LEADING(i t ex) USE_NL(t) USE_HASH(ex) SWAP_JOIN_INPUTS(ex) NO_MERGE(ex) NO_PUSH_PRED(ex) INDEX(i "
                + versionIndex + ") INDEX(t " + targetFkIndex + ") */";
    }

    public static Optional<SixFileKind> of(ImportFileType type) {
        return Arrays.stream(values()).filter(k -> k.importFileType == type).findFirst();
    }

    public static Optional<SixFileKind> ofFileName(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        String upper = fileName.toUpperCase();
        return Arrays.stream(values()).filter(k -> upper.contains(k.fileNameMarker)).findFirst();
    }

    public static Optional<SixFileKind> ofRootIsin(String rootIsinColumn) {
        return Arrays.stream(values()).filter(k -> k.rootIsinColumn.equalsIgnoreCase(rootIsinColumn)).findFirst();
    }

    static final class Names {
        private final String fileNameMarker;
        private final String filterFileType;
        private final String pollerFileType;

        Names(String fileNameMarker, String filterFileType, String pollerFileType) {
            this.fileNameMarker = fileNameMarker;
            this.filterFileType = filterFileType;
            this.pollerFileType = pollerFileType;
        }
    }

    static final class Tables {
        private final String table;
        private final String instrumentLinkColumn;
        private final String rootIsinColumn;
        private final String targetFkColumn;
        private final String filteredTable;
        private final String versionIndex;
        private final String targetFkIndex;

        Tables(String table, String instrumentLinkColumn, String rootIsinColumn, String targetFkColumn,
                String filteredTable, String versionIndex, String targetFkIndex) {
            this.table = table;
            this.instrumentLinkColumn = instrumentLinkColumn;
            this.rootIsinColumn = rootIsinColumn;
            this.targetFkColumn = targetFkColumn;
            this.filteredTable = filteredTable;
            this.versionIndex = versionIndex;
            this.targetFkIndex = targetFkIndex;
        }
    }
}
