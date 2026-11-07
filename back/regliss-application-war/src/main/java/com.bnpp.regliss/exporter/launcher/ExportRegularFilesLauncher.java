package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.exporter.GeneratedFileNameService;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import com.bnpp.regliss.exporter.writer.FileGenerator;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.ListsPVLivraison;
import com.bnpp.regliss.exporter.writer.format.regular.RegularFileExportData;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.VersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

@Service
@ReglissBatchProfile
@Slf4j
public class ExportRegularFilesLauncher {

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private FileGenerator generator;

    @Autowired
    private ExportDataExtractor exportDataExtractor;

    @Autowired
    private GeneratedFileNameService fileNameService;

    public Map<GeneratedFile, FileGenerationTry> generateVersionRegularFiles(ExportJobParameters exportParameters, Version version) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        GenerateRegularFiles filesToGenerate = getRegularFilesToGenerate(exportParameters);

        generatedFiles.putAll(generateVersionDataFiles(exportParameters, version));
        filesToGenerate.getDeliveryReportFiles().map(deliveryReport -> tryGenerateDeliveryReport(deliveryReport, exportParameters))
                .forEach(generatedFiles::putAll);
        generatedFiles.putAll(filesToGenerate.getFircosoftZipFile().map(file -> tryGenerateFircosoftZip(file, generatedFiles, version,
                exportParameters)).orElse(emptyMap()));

        return generatedFiles;
    }

    public Map<GeneratedFile, FileGenerationTry> generateConcatenatedListRegularFiles(ExportJobParameters exportParameters,
                                                                                      List<Long> reglissListIds) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        GenerateRegularFiles toGenerate = getRegularFilesToGenerate(exportParameters);
        Map<FileExportMode, List<GeneratedFile>> dataFilesByExportMode = toGenerate.getDataFilesByExportMode();

        for (Entry<FileExportMode, List<GeneratedFile>> exportModeToFiles : dataFilesByExportMode.entrySet()) {
            long exportModeStartTime = currentTimeMillis();

            RegularFileExportData regularExportParameters = getRegularRecordsConcatenatedListExportData(exportParameters,
                    exportModeToFiles.getKey(), reglissListIds);
            generatedFiles.putAll(generator.tryGenerateRegularFiles(exportModeToFiles.getValue(), regularExportParameters, exportParameters));
            generatedFiles.putAll(toGenerate.getFircosoftZipFile().map(file -> tryGenerateFircosoftZipForConcatenatedList(file,
                    generatedFiles, exportParameters)).orElse(emptyMap()));
            log.debug("Generating all files for Export Mode {} finished in {} seconds", exportModeToFiles.getKey(),
                    (currentTimeMillis() - exportModeStartTime) / 1000);
        }
        return generatedFiles;
    }

    private Map<GeneratedFile, FileGenerationTry> generateVersionDataFiles(ExportJobParameters exportParameters, Version version) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        GenerateRegularFiles toGenerate = getRegularFilesToGenerate(exportParameters);
        Map<FileExportMode, List<GeneratedFile>> dataFilesByExportMode = toGenerate.getDataFilesByExportMode();

        for (Entry<FileExportMode, List<GeneratedFile>> exportModeToFiles : dataFilesByExportMode.entrySet()) {
            long exportModeStartTime = currentTimeMillis();

            RegularFileExportData regularExportParameters = getRegularRecordsVersionExportData(exportParameters, version,
                    exportModeToFiles.getKey());
            generatedFiles.putAll(generator.tryGenerateRegularFiles(exportModeToFiles.getValue(), regularExportParameters, exportParameters));

            log.debug("Generating all files for Export Mode {} finished in {} seconds", exportModeToFiles.getKey(),
                    (currentTimeMillis() - exportModeStartTime) / 1000);
        }
        return generatedFiles;
    }

    private RegularFileExportData getRegularRecordsVersionExportData(ExportJobParameters exportParameters, Version version,
                                                                     FileExportMode exportMode) {
        Optional<Version> previousVersion = versionRepo.getPreviousVersionByNumber(version.getList().getId(), version.getVersionNumber());
        Map<Long, Version> extractedIDs = exportDataExtractor.extractVersionRecordIdsForRegularExportFiles(version,
                exportMode.getActualExportMode(previousVersion.isPresent())); // ONCE per export mode
        progressService.incrementBatchExportProgressAfterExtractData(exportParameters, ExportQueryType.VERSION_REGULAR_FILES_EXPORT_QUERY);
        return new RegularFileExportData(extractedIDs, exportMode, version);
    }

    private RegularFileExportData getRegularRecordsConcatenatedListExportData(ExportJobParameters exportParameters,
                                                                              FileExportMode exportMode, List<Long> reglissListIds) {
        Map<Long, Version> concatenatedListRecordIds = exportDataExtractor.extractConcatenatedListRecordIds(reglissListIds); // ONCE per export
        progressService.incrementBatchExportProgressAfterExtractData(exportParameters,
                ExportQueryType.CONCATENATED_LIST_REGULAR_FILES_EXPORT_QUERY);
        return new RegularFileExportData(concatenatedListRecordIds, exportMode, null);
    }

    private Map<GeneratedFile, FileGenerationTry> tryGenerateDeliveryReport(GeneratedFile deliveryReport,
                                                                            ExportJobParameters exportJobParameters) {
        Set<ReglissList> listsToBeIncluded = exportDataExtractor.extractListsForDeliveryReport(deliveryReport);
        progressService.incrementBatchExportProgressAfterExtractData(exportJobParameters, ExportQueryType.DELIVERY_REPORT_QUERY);

        FileGenerationTry fileTry = generator.tryGenerateDeliveryReportFile(deliveryReport, listsToBeIncluded, exportJobParameters);
        return singletonMap(deliveryReport, fileTry);
    }

    private Map<GeneratedFile, FileGenerationTry> tryGenerateFircosoftZip(GeneratedFile fircosoftZip, Map<GeneratedFile,
            FileGenerationTry> generatedFiles, Version version, ExportJobParameters exportJobParameters) {
        Map<String, FileGenerationTry> zipFiles = generatedFiles.keySet().stream().
                filter(gf -> gf.getGeneratedFileFormat().getFileFormat() == FileExportFormat.FIRCOSOFT)
                .collect(toMap(GeneratedFile::getFullName, generatedFiles::get));

        ListsPVLivraison fircosoftPVLists = exportDataExtractor.extractFircosoftPVDeliveryLists(version);
        progressService.incrementBatchExportProgressAfterExtractData(exportJobParameters, ExportQueryType.FIRCOSOFT_PV_DELIVERY_EXPORT_QUERY);

        String pvFileName = fileNameService.getFircosoftXmlPVFilename() + ".xml";
        FileGenerationTry pvXmlTry = generator.tryGenerateFircosoftPVXml(pvFileName, fircosoftPVLists, version);
        zipFiles.put(pvFileName, pvXmlTry);

        String logFileName = fileNameService.getFircosoftLogFilename() + ".log";
        FileGenerationTry logTry = generator.tryGenerateFircosoftLog(zipFiles, logFileName, version);
        zipFiles.put(logFileName, logTry);

        FileGenerationTry fircosoftZipTry = generator.tryGenerateZip(fircosoftZip, zipFiles, exportJobParameters);
        return singletonMap(fircosoftZip, fircosoftZipTry);
    }

    private Map<GeneratedFile, FileGenerationTry> tryGenerateFircosoftZipForConcatenatedList(GeneratedFile fircosoftZip,
                           Map<GeneratedFile, FileGenerationTry> generatedFiles, ExportJobParameters exportJobParameters) {
        Map<String, FileGenerationTry> zipFiles = generatedFiles.keySet().stream().
                filter(gf -> gf.getGeneratedFileFormat().getFileFormat() == FileExportFormat.FIRCOSOFT)
                .collect(toMap(GeneratedFile::getFullName, generatedFiles::get));

        String logFileName = fileNameService.getFircosoftLogFilename() + ".log";
        FileGenerationTry logTry = generator.tryGenerateFircosoftLogForConcatenatedList(zipFiles, logFileName);
        zipFiles.put(logFileName, logTry);

        FileGenerationTry fircosoftZipTry = generator.tryGenerateZip(fircosoftZip, zipFiles, exportJobParameters);
        return singletonMap(fircosoftZip, fircosoftZipTry);
    }

    private GenerateRegularFiles getRegularFilesToGenerate(ExportJobParameters exportParameters) {
        return exportParameters.regularFiles()
                .orElseThrow(() -> new ReglissException("Required regular files not present: " + exportParameters));
    }

}
