package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Subscription;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.GeneratedFileNameService;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.*;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import com.bnpp.regliss.exporter.writer.FileGenerator;
import com.bnpp.regliss.exporter.writer.format.dj.DJCoreFileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.DJWatchlistAmeFileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.ListsWatchlistPVLivraison;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.DJExportFilterRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.vo.AssociationExternalReference;
import com.bnpp.regliss.vo.FilteringReferentials;
import com.bnpp.regliss.vo.RecordIdType;
import com.bnpp.regliss.vo.export.dj.watchlist.FilterIdTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;

@Service
@ReglissBatchProfile
public class ExportDJFormatLauncher {

    @Autowired
    private FileGenerator generator;

    @Autowired
    private ExportDataExtractor exportDataExtractor;

    @Autowired
    private RecordsExportFilteringService recordsExportFilteringService;

    @Autowired
    private AssociationsFilteringService associationsFilteringService;

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private DJExportFilterRepository filterRepo;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private GeneratedFileNameService fileNameService;

    public Map<GeneratedFile, FileGenerationTry> generateVersionDJFiles(ExportJobParameters exportParameters, Version version,
                                                                        List<DJExportFilter> allFilters) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();

        generatedFiles.putAll(generateDjCoreFiles(exportParameters, version));

        generatedFiles.putAll(generateDjWatchlistAmeFiles(exportParameters, version, allFilters));

        return generatedFiles;
    }

    private Map<GeneratedFile, FileGenerationTry> generateDjCoreFiles(ExportJobParameters exportParameters, Version version) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        GenerateDJFiles djFilesToGenerate = getDjFilesToGenerate(exportParameters);

        Map<FileExportMode, List<GeneratedFile>> djCoreFilesByExportMode = djFilesToGenerate.getDjCoreFilesByExportMode();

        for (Entry<FileExportMode, List<GeneratedFile>> exportModeFiles : djCoreFilesByExportMode.entrySet()) {
            DJCoreFileExportData djCoreExportParameters = getDjCoreRecordsExportData(exportParameters, version, exportModeFiles.getKey());
            generatedFiles.putAll(generator.tryGenerateDjFiles(exportModeFiles.getValue(), djCoreExportParameters, exportParameters));
        }
        return generatedFiles;
    }

    private Map<GeneratedFile, FileGenerationTry> generateDjWatchlistAmeFiles(ExportJobParameters exportParameters, Version version,
                                                                               List<DJExportFilter> allFilters) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        Set<GeneratedFile> filesToGenerate = getDjFilesToGenerate(exportParameters).getDjWatchlistAmeFiles();
        if (!filesToGenerate.isEmpty()) {
            DJWatchlistAmeFileExportData djWatchlistAmeExportParameters = getDjWatchlistAmeRecordsExportData(exportParameters,
                    version,allFilters);
            generatedFiles.putAll(generator.tryGenerateDjFiles(filesToGenerate, djWatchlistAmeExportParameters, exportParameters));
            generatedFiles.putAll(generateWatchlistZipFiles(generatedFiles, exportParameters, version));
        }

        return generatedFiles;
    }

    private DJWatchlistAmeFileExportData getDjWatchlistAmeRecordsExportData(ExportJobParameters exportParameters, Version version,
                                                                            List<DJExportFilter> allFilters) {
        WatchlistAmeOriginalCategoryIds categoryIds = exportDataExtractor.getCategoryIdsInOriginalImportFile(version);
        final RecordsToFiltersAndAssociations filteredRecordBuckets = filterRecords(exportParameters, version, allFilters);
        List<FilterIdTag> filterTags = allFilters.stream().map(DJExportFilter::getFilterIdTag).collect(toList());

        WatchlistAmeFilterTagsToDescription2Details watchlistAmeFilterTagsToDescription2Details =
                new WatchlistAmeFilterTagsToDescription2Details(filterTags, version);

        RecordsBuckets recordsBuckets = new RecordsBuckets(filteredRecordBuckets, filterTags);

        return new DJWatchlistAmeFileExportData(recordsBuckets, watchlistAmeFilterTagsToDescription2Details, version, categoryIds);
    }

    private DJCoreFileExportData getDjCoreRecordsExportData(ExportJobParameters exportParameters, Version version, FileExportMode exportMode) {
        Optional<Version> previousVersion = versionRepo.getPreviousVersionByNumber(version.getList().getId(), version.getVersionNumber());
        Set<RecordIdType> recordIds = exportDataExtractor.extractRecordsForCore(exportMode.getActualExportMode(previousVersion.isPresent()),
                version); // ONCE per export mode
        progressService.incrementBatchExportProgressAfterExtractData(exportParameters, ExportQueryType.CORE_EXPORT_QUERY);
        return new DJCoreFileExportData(recordIds, exportMode, version);
    }

    private RecordsToFiltersAndAssociations filterRecords(ExportJobParameters exportParameters, Version version, List<DJExportFilter> allFilters) {

        RecordsToFiltersAndAssociations fullAllFilteredRecordsBuckets = filterRecordsForWatchlistAmeFilesFullAll(exportParameters, version);
        RecordsToFiltersAndAssociations notFullAllFilteredRecordsBuckets = filterRecordsForWatchlistAmeFilesNotFullAll(exportParameters, version, allFilters);

        return fullAllFilteredRecordsBuckets.union(notFullAllFilteredRecordsBuckets);
    }

    private RecordsToFiltersAndAssociations filterRecordsForWatchlistAmeFilesFullAll(ExportJobParameters exportParameters, Version version) {
        List<GeneratedFile> watchlistAmeFilesFullExportingAll = getDjFilesToGenerate(exportParameters).getWatchlistAmeFilesFullExportingAll();

        if (!watchlistAmeFilesFullExportingAll.isEmpty()) {
            Map<RecordIdType, List<AssociationExternalReference>> filteredRecordToAssociations =
                    exportDataExtractor.extractWatchlistAmeFullRecordsToAssociations(version);
            Set<Long> filterIds = watchlistAmeFilesFullExportingAll.stream()
                    .map(gf -> gf.getGeneratedFileFormat().getFilter().getId())
                    .collect(Collectors.toSet());
            Map<Long, RecordBucketContent> recordFilteredData = filteredRecordToAssociations.entrySet().stream()
                    .collect(toMap(e -> e.getKey().getId(), e -> mapFiltersToAssociations(filterIds,
                            e.getValue(), e.getKey().getType(),e.getKey().getCreatedInVersion(), e.getKey().getClonedInversion())));

            progressService.incrementBatchExportProgressAfterExtractData(exportParameters, ExportQueryType.WATCHLIST_AME_FULL_ALL_EXPORT_QUERY);
            return new RecordsToFiltersAndAssociations(recordFilteredData);
        }
        return new RecordsToFiltersAndAssociations(emptyMap());
    }

    private RecordBucketContent mapFiltersToAssociations(Set<Long> filterIds, List<AssociationExternalReference> associations,
                                                         RecordType type,Long createdInVersion, Long clonedInVersion) {
        Map<Long, List<AssociationExternalReference>> filtersToAssociations = filterIds.stream().collect(toMap(Function.identity(),
                filterId -> associations));
        return new RecordBucketContent(filtersToAssociations, type,createdInVersion,clonedInVersion);
    }

    private RecordsToFiltersAndAssociations filterRecordsForWatchlistAmeFilesNotFullAll(ExportJobParameters exportParameters,
                                                                                        Version version, List<DJExportFilter> allFilters) {
        GenerateDJFiles djFilesToGenerate = getDjFilesToGenerate(exportParameters);

        Map<FileExportMode, List<GeneratedFile>> watchlistAmeFilesNotExportingFullAllByExportMode =
                djFilesToGenerate.getWatchlistAmeFilesNotExportingFullAllByExportMode();
        Map<FileExportMode, List<DJExportFilter>> filterByExportMode = allFilters.stream().collect(groupingBy(DJExportFilter::getType));

        RecordsToFiltersAndAssociations recordFilteredData = new RecordsToFiltersAndAssociations();
        if (!watchlistAmeFilesNotExportingFullAllByExportMode.isEmpty()) {
            FilteringReferentials filteringReferentials = exportDataExtractor.getFilteringReferentials(); // ONCE
            AssociationsFilteringData associationsFilteringData = exportDataExtractor.extractAssociationsFilteringData(djFilesToGenerate,
                    version); //ONCE

            for ( Map.Entry<FileExportMode, List<GeneratedFile>> pair : watchlistAmeFilesNotExportingFullAllByExportMode.entrySet()) {
                List<RecordFilteringData> recordFilteringData = exportDataExtractor.extractDjWatchlistAmeRecordFilteringData(version,
                        pair.getKey()); // ONCE per export mode
                progressService.incrementBatchExportProgressAfterExtractData(exportParameters,
                        ExportQueryType.WATCHLIST_AME_NOT_FULL_ALL_EXPORT_QUERY);

                ExportWatchlistAmeFilteringData filteringDataForExportMode = new ExportWatchlistAmeFilteringData(recordFilteringData,
                        filteringReferentials);

                List<DJExportFilter> filtersByFile = filterByExportMode.get(pair.getKey());
                for (DJExportFilter filter : filtersByFile){
                    Map<RecordIdType, ExportedRelationTypes> filteredRecordIds = recordsExportFilteringService.filterRecordsInMemory(filter,
                            filteringDataForExportMode);
                    Map<RecordIdType, List<AssociationExternalReference>> filteredRecordToAssociations =
                            associationsFilteringService.filterAssociationsInMemory(filter, filteredRecordIds, associationsFilteringData);
                    filteredRecordToAssociations.forEach(
                            (recordId, associations) -> recordFilteredData.merge(recordId.getId(),
                                    mapAssociationsToFilterId(filter.getId(), associations, recordId.getType(),recordId.getCreatedInVersion(),
                                            recordId.getClonedInversion())));
                }
            }
        }
        return recordFilteredData;
    }

    private RecordBucketContent mapAssociationsToFilterId(Long filterId, List<AssociationExternalReference> associations,
                                                          RecordType type,Long createdInVersion, Long clonedInVersion) {
        Map<Long, List<AssociationExternalReference>> associationsMap = new HashMap<>();
        associationsMap.put(filterId, associations);
        return new RecordBucketContent(associationsMap, type,createdInVersion,clonedInVersion);
    }

    private Map<GeneratedFile, FileGenerationTry> generateWatchlistZipFiles(Map<GeneratedFile, FileGenerationTry> generatedFiles,
                                                                            ExportJobParameters exportJobParameters, Version version) {
        Map<GeneratedFile, FileGenerationTry> watchlistZipGeneratedFiles = new HashMap<>();


        Set<GeneratedFile> watchlistZipFiles = getDjFilesToGenerate(exportJobParameters).getWatchlistZipFiles();
        if(!watchlistZipFiles.isEmpty()) {

            Map<GeneratedFile, FileGenerationTry> generatedXmlFiles=generatedFiles.entrySet().stream()
                    .filter(entry->entry.getKey().getGeneratedFileFormat().getFileExtension().equals(FileExportExtension.xml))
                    .collect(toMap(Entry::getKey,Entry::getValue));

            for (Entry<GeneratedFile, FileGenerationTry> entry : generatedXmlFiles.entrySet()) {

                Optional<GeneratedFile> watchlistZipForFilter = watchlistZipFiles.stream()
                        .filter(zip -> zip.getGeneratedFileFormat().getFilter().getFilterName()
                                .equals(entry.getKey().getGeneratedFileFormat().getFilter().getFilterName()))
                        .findFirst();
                ListsWatchlistPVLivraison watchListPVLists = exportDataExtractor.extractWatchlistPVDeliveryLists(version,
                        exportJobParameters.getComputedValuesForFile(entry.getKey().getFullName()));
                watchlistZipForFilter.ifPresent(
                        zipToGenerate -> watchlistZipGeneratedFiles.put(zipToGenerate, generateWatchlistZip(zipToGenerate, entry,
                                watchListPVLists, version, exportJobParameters))
                );

            }

        }
        return watchlistZipGeneratedFiles;
    }

    private FileGenerationTry generateWatchlistZip(GeneratedFile zipToGenerate, Entry<GeneratedFile, FileGenerationTry> generatedFilterFile,
                                                   ListsWatchlistPVLivraison watchListPVLists, Version version,
                                                   ExportJobParameters exportJobParameters) {
        Map<String, FileGenerationTry> zipFiles = new HashMap<>();
        zipFiles.put(fileNameService.getWatchlistXmlZip(zipToGenerate.getFileName(), version) + ".xml", generatedFilterFile.getValue());

        String pvXmlFilename = fileNameService.getWatchlistXmlPVFilename() + ".xml";
        FileGenerationTry pvXmlTry = generator.tryGenerateWatchlistPVXml(pvXmlFilename, watchListPVLists, version);
        zipFiles.put(pvXmlFilename, pvXmlTry);

        String logFilename = fileNameService.getWatchlistLogFilename() + ".txt";
        Set<Subscription> listSubscriptions = exportDataExtractor.getSubscriptionsForList(generatedFilterFile.getKey()
                .getGeneratedFileFormat().getFilter());
        FileGenerationTry logTry = generator.tryGenerateWatchlistLog(generatedFilterFile.getKey().getGeneratedFileFormat().getFilter(),
                listSubscriptions, zipFiles, logFilename, version);
        zipFiles.put(logFilename, logTry);

        return generator.tryGenerateZip(zipToGenerate, zipFiles, exportJobParameters);
    }

    private GenerateDJFiles getDjFilesToGenerate(ExportJobParameters exportParameters) {
        return exportParameters.djFiles()
                .orElseThrow(() -> new ReglissException("Required DJ files not present: " + exportParameters));
    }

}
