package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static com.bnpp.regliss.entity.referential.FileExportExtension.csv;
import static com.bnpp.regliss.entity.referential.FileExportExtension.xml;
import static com.bnpp.regliss.entity.referential.FileExportFormat.*;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public enum ExportPageType {

    // NOTE: "ImmutableMap.<~>builder()" is IntelliJ folding; written out as <FileExportFormat, List<FileExportExtension>>
    RECORDS_FULLY_LOADED_PAGE(ExportPhase.EXPORT_WRITE_RECORDS,
            ImmutableMap.<FileExportFormat, List<FileExportExtension>>builder()
            .put(GENERIC, GENERIC.getExtensions())
            .put(REGLISS, REGLISS.getExtensions())
            .put(VIGILANCE, REGLISS.getExtensions())
            .put(FIRCOSOFT, FIRCOSOFT.getExtensions())
            .put(SAFEWATCH, SAFEWATCH.getExtensions())
            .build()),

    RECORDS_XML_SNIPPETS_PAGE(ExportPhase.EXPORT_WRITE_RECORDS,
            ImmutableMap.<FileExportFormat, List<FileExportExtension>>builder()
            .put(DJ_WATCHLIST, singletonList(xml))
            .put(DJ_CORE, singletonList(xml))
            .build()),

    RECORDS_CSV_SNIPPETS_PAGE(ExportPhase.EXPORT_WRITE_RECORDS,
            ImmutableMap.<FileExportFormat, List<FileExportExtension>>builder()
            .put(DJ_WATCHLIST, singletonList(csv))
            .put(DJ_CORE, singletonList(csv))
            .build()),

    ASSOCIATIONS_XML_SNIPPETS_PAGE(ExportPhase.EXPORT_WRITE_ASSOCIATIONS,
            ImmutableMap.<FileExportFormat, List<FileExportExtension>>builder()
            .put(DJ_WATCHLIST, singletonList(xml))
            .build())

    ;

    private final Map<FileExportFormat, List<FileExportExtension>> availableFormatsAndExtensions;
    @Getter
    private final ExportPhase exportPhase;

    ExportPageType(ExportPhase exportPhase, Map<FileExportFormat, List<FileExportExtension>> availableFormatsAndExtensions) {
        this.exportPhase = exportPhase;
        this.availableFormatsAndExtensions = availableFormatsAndExtensions;
    }

    public static ExportPageType getDJPageTypeByPhaseAndExtension(ExportPhase exportPhase, FileExportExtension extension) {
        return Stream.of(values()).filter(pageType -> pageType.exportPhase == exportPhase)
                .filter(pageType -> pageType.getDJAvailableExtensions().contains(extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No page type for export phase " + exportPhase +" and extension " + extension));
    }

    public List<ExportPageType> getAllPageTypesForSamePhaseAndExtension(){
        return Stream.of(values()).filter(pageType -> pageType.exportPhase == this.getExportPhase())
                .filter(pageType -> pageType.getAvailableExtensions().equals(this.getAvailableExtensions()))
                .collect(toList());
    }

    private List<FileExportExtension> getDJAvailableExtensions(){
        return availableFormatsAndExtensions.entrySet().stream()
                .filter(e -> e.getKey().isDJFormat())
                .map(Entry::getValue)
                .flatMap(List::stream).collect(toList());
    }

    private List<FileExportExtension> getAvailableExtensions(){
        return availableFormatsAndExtensions.values().stream().flatMap(List::stream).collect(toList());
    }

}
