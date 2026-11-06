package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.entity.ExportFile;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.exporter.launcher.ExportQueryType;
import com.bnpp.regliss.exporter.launcher.GenerateDJFiles;
import com.bnpp.regliss.exporter.launcher.GenerateRegularFiles;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import lombok.Getter;

import java.util.*;

public class ExportJobParameters {
    @Getter
    private final long batchJobExecId;

    private final ExportFilesMetadata exportFilesMetadata;

    private float overallPercentage = 00.00f;
    @Getter
    private float lastPrintedOverallPercentage = 0f;
    private Map<String,Map<CounterKeys,Long>> statistics=new HashMap<>();

    public ExportJobParameters(Collection<GeneratedFile> filesToGenerate, boolean hasPreviousVersion, long batchJobExecId) {
        this.exportFilesMetadata = new ExportFilesMetadata(filesToGenerate, hasPreviousVersion);
        this.batchJobExecId = batchJobExecId;
    }

    public ExportJobParameters(AddModSupExportFile addModSupExportFile, long batchJobExecId) {
        this.batchJobExecId = batchJobExecId;
        this.exportFilesMetadata = new ExportFilesMetadata(addModSupExportFile);
    }

    public ExportJobParameters(AddModSupAuditTrailFile addModSupAuditTrailFile, long batchJobExecId) {
        this.batchJobExecId = batchJobExecId;
        this.exportFilesMetadata = new ExportFilesMetadata(addModSupAuditTrailFile);
    }

    public Optional<GenerateRegularFiles> regularFiles() { return exportFilesMetadata.regularFiles(); }

    public Optional<GenerateDJFiles> djFiles() { return exportFilesMetadata.djFiles(); }

    public Optional<AddModSupExportFile> addModSupExportFile() { return exportFilesMetadata.addModSupExportFile(); }

    public Optional<AddModSupAuditTrailFile> addModSupAuditTrailFile() { return exportFilesMetadata.addModSupAuditTrailFile(); }

    public int getNumberOfDJFiles() {
        return djFiles().map(djFiles -> djFiles.getAllFiles().size())
                .orElse(0);
    }

    public int getNumberOfDJFilesNotZip() {
        return (int) djFiles().map(djFiles -> djFiles.getAllFiles().stream().filter(file -> file.getGeneratedFileFormat().getFileExtension() !=
                        FileExportExtension.zip).count())
                .orElse(0L)
                .longValue();
    }

    public int getNumberOfDJFileZip() {
        return (int) djFiles().map(djFiles -> djFiles.getAllFiles().stream().filter(file -> file.getGeneratedFileFormat().getFileExtension() ==
                        FileExportExtension.zip).count())
                .orElse(0L)
                .longValue();
    }

    public int getNumberOfDJXmlFiles() {
        return (int) djFiles()
                .map(djFiles -> djFiles.getAllFiles().stream().filter(file -> file.getGeneratedFileFormat().getFileExtension() ==
                        FileExportExtension.xml).count())
                .orElse(0L)
                .longValue();
    }

    public int getNumberOfDJWatchlistAmeXmlFiltersFiles() {
        return (int) djFiles()
                .map(djFiles -> djFiles.getDjWatchlistAmeFiles().stream().filter(file -> file.getGeneratedFileFormat().getFileExtension() ==
                        FileExportExtension.xml).count())
                .orElse(0L)
                .longValue();
    }

    public int getNumberOfQueries() {
        return djFiles().map(djFiles -> getNumberOfQueries(djFiles.queryTypesToFiles())).orElse(0) +
                regularFiles().map(regularFiles -> getNumberOfQueries(regularFiles.queryTypesToFiles())).orElse(0) +
                getNumberOfAddModSupFiles() +
                getNumberOfAddModSupAuditTrailFiles();
    }

    private int getNumberOfQueries(Map<ExportQueryType, List<GeneratedFile>> queriesToFiles) {
        return queriesToFiles.values().stream()
                .mapToInt(this::getNumberOfExportModesForDataFiles)
                .sum();
    }

    private int getNumberOfExportModesForDataFiles(List<GeneratedFile> dataFiles) {
        return (int) dataFiles.stream()
                .map(GeneratedFile::getFileExportMode).distinct().count();
    }

    public int getTotalNumberOfFiles() {
        return getNumberOfAddModSupFiles() +
                getNumberOfAddModSupAuditTrailFiles() +
                getNumberOfDJFiles() +
                getNumberOfRegularFiles();
    }

    public int getNumberOfAddModSupFiles() { return addModSupExportFile().map(addModSupExportFile -> 1).orElse(0); }

    public int getNumberOfAddModSupAuditTrailFiles() { return addModSupAuditTrailFile().map(addModSupAuditTrailFile -> 1).orElse(0);
    }

    public int getNumberOfRegularFiles() {
        return regularFiles().map(regularFiles -> regularFiles.getAllFiles().size()).orElse(0);
    }

    public float incrementOverallPercentage(float percentToIncrement) {
        this.overallPercentage += percentToIncrement;
        return get2DecimalFloatValue(overallPercentage);
    }

    // [GAP] about 12 lines (~119-131) not in the photos; the next visible line (131) is the closing "}" of a method

    public void updateLastPrintedOverallPercentage(float overallPercentageAfterStep) {
        lastPrintedOverallPercentage = overallPercentageAfterStep;
    }

    private float get2DecimalFloatValue(float value) {
        return Float.parseFloat(String.format(Locale.ENGLISH, "%.2f", value));
    }

    public Map<ExportQueryType, List<ExportFile>> queryTypesToFiles() {
        return exportFilesMetadata.queryTypesToFiles();
    }

    public void addStatistics(String generatedFile, Map<CounterKeys,Long> computedValues){
        this.statistics.put(generatedFile,computedValues);
    }
    public Map<CounterKeys,Long> getComputedValuesForFile(String generatedFile){
        return this.statistics.get(generatedFile);
    }
}
