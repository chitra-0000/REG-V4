package com.bnpp.regliss.entity.referential;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ImportFileType;

import java.util.List;

import static com.bnpp.regliss.entity.referential.FileExportExtension.*;
import static com.bnpp.regliss.entity.referential.FileExportMode.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

public enum FileExportFormat {
    GENERIC(singletonList(csv),  asList(FULL, SYNTHESIS_OF_ADD, SYNTHESIS_OF_MOD, SYNTHESIS_OF_SUPP), true, false),
    REGLISS(singletonList(csv), asList(FULL, ADD_MOD, ADD, MOD, SUPP), true, false), // FileExportMode
    VIGILANCE(asList(txt, csv), asList(FULL, ADD_MOD, ADD, MOD, SUPP), true, false),
    SAFEWATCH(singletonList(txt), asList(FULL, ADD_MOD, ADD, MOD, SUPP), false, false),
    FIRCOSOFT(asList(txt, alt, add, zip), singletonList(FULL), false, true),
    DJ_WATCHLIST(asList(csv, xml,zip), asList(FULL, ADD_MOD), false, true),
    DJ_CORE(singletonList(xml), asList(FULL, ADD_MOD), false, true),
    DJ_AME(singletonList(xml), asList(FULL, ADD_MOD), false, true),
    DELIVERY_REPORT(singletonList(xml), singletonList(FULL), false, true);

    private final List<FileExportExtension> extensions;
    private final boolean supportsGlobal;
    private final List<FileExportMode> exportModes;
    private boolean isIncrementalEmptyFileGenerated;

    FileExportFormat(List<FileExportExtension> extensions, List<FileExportMode> exportModes,
                boolean supportsGlobal, boolean isIncrementalEmptyFileGenerated) {
        this.extensions = unmodifiableList(extensions);
        this.exportModes = unmodifiableList(exportModes);
        this.supportsGlobal = supportsGlobal;
        this.isIncrementalEmptyFileGenerated = isIncrementalEmptyFileGenerated;
    }

    public ImportFileType getDJFormat() {
        switch (this) {
            case DJ_WATCHLIST:
                return ImportFileType.DOW_JONES_WATCHLIST;
            case DJ_CORE:
                return ImportFileType.DOW_JONES_CORE;
            case DJ_AME:
                return ImportFileType.DOW_JONES_AME;
            default:
                throw new ReglissException("The file format is not a DJ format");
        }
    }

    public List<FileExportExtension> getExtensions() { return unmodifiableList(extensions); }

    public boolean isSupportsGlobal() { return supportsGlobal; }

    public boolean isIncrementalEmptyFileGenerated() { return isIncrementalEmptyFileGenerated; }

    public List<FileExportMode> getExportModes() { return unmodifiableList(exportModes); }

    public boolean isDJFormat() { return this == DJ_CORE || this == DJ_WATCHLIST || this == FileExportFormat.DJ_AME; }
}
