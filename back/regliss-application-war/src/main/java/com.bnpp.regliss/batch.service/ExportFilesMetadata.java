package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.entity.ExportFile;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.exporter.launcher.ExportQueryType;
import com.bnpp.regliss.exporter.launcher.GenerateDJFiles;
import com.bnpp.regliss.exporter.launcher.GenerateRegularFiles;
import lombok.ToString;

import java.util.*;
import java.util.Map.Entry;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

@ToString
class ExportFilesMetadata {
    private final GenerateRegularFiles regularFiles;
    private final GenerateDJFiles djFiles;
    private final AddModSupExportFile addModSupExportFile;
    private final AddModSupAuditTrailFile addModSupAuditTrailFile;

    public ExportFilesMetadata(Collection<GeneratedFile> filesToGenerate, boolean hasPreviousVersion) {
        Set<GeneratedFile> djFilesToGenerate = filesToGenerate.stream().filter(GeneratedFile::isDJFormat).collect(toSet());
        Set<GeneratedFile> regularFilesToGenerate = filesToGenerate.stream().filter(GeneratedFile::isRegularFormat).collect(toSet());
        this.djFiles = new GenerateDJFiles(djFilesToGenerate, hasPreviousVersion);
        this.regularFiles = new GenerateRegularFiles(regularFilesToGenerate);
        this.addModSupExportFile = null;
        this.addModSupAuditTrailFile = null;

        checkFilesHaveCorrespondingQuery(filesToGenerate);
    }

    public ExportFilesMetadata(AddModSupExportFile addModSupExportFile) {
        this.addModSupExportFile = addModSupExportFile;
        this.addModSupAuditTrailFile = null;
        this.djFiles = null;
        this.regularFiles = null;

        checkFilesHaveCorrespondingQuery(singletonList(addModSupExportFile));
    }
    public ExportFilesMetadata(AddModSupAuditTrailFile addModSupAuditTrailFile) {
        this.addModSupAuditTrailFile = addModSupAuditTrailFile;
        this.addModSupExportFile = null;
        this.djFiles = null;
        this.regularFiles = null;

        checkFilesHaveCorrespondingQuery(singletonList(addModSupAuditTrailFile));
    }

    private void checkFilesHaveCorrespondingQuery(Collection<? extends ExportFile> filesToGenerate) {
        List<? extends ExportFile> filesWithQueries = queryTypesToFiles().values().stream().flatMap(List::stream).collect(toList());
        filesWithQueries.removeAll(filesToGenerate);//NOSONAR types validated by generics
        if (!filesWithQueries.isEmpty()) {
            throw new IllegalStateException("Not all files have a corresponding query: " + filesWithQueries);
        }
    }

    Optional<GenerateRegularFiles> regularFiles() { return Optional.ofNullable(regularFiles); }

    Optional<GenerateDJFiles> djFiles() { return Optional.ofNullable(djFiles); }

    Optional<AddModSupExportFile> addModSupExportFile() { return Optional.ofNullable(addModSupExportFile); }

    Optional<AddModSupAuditTrailFile> addModSupAuditTrailFile() { return Optional.ofNullable(addModSupAuditTrailFile); }

    public Map<ExportQueryType, List<ExportFile>> queryTypesToFiles() {
        Map<ExportQueryType, List<ExportFile>> result = new HashMap<>();
        djFiles().ifPresent(djFile-> result.putAll(mapQueryTypeToExportFiles(djFiles.queryTypesToFiles())));
        regularFiles().ifPresent(regFiles -> result.putAll(mapQueryTypeToExportFiles(regFiles.queryTypesToFiles())));
        addModSupExportFile().ifPresent(addModSupFile -> result.put(ExportQueryType.ADD_MOD_SUP_EXPORT_QUERY, singletonList(addModSupFile)));
        addModSupAuditTrailFile().ifPresent(addModSupAuditFile -> result.put(ExportQueryType.ADD_MOD_SUP_AUDIT_TRAIL_QUERY,
                singletonList(addModSupAuditFile)));

        return result;
    }

    private Map<ExportQueryType, List<ExportFile>> mapQueryTypeToExportFiles(Map<ExportQueryType, List<GeneratedFile>> exportQueryTypeListMap) {
        return exportQueryTypeListMap.entrySet().stream()
                .collect(toMap(Entry::getKey, e -> (List) new ArrayList(e.getValue())));
    }
}
