package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.batch.service.ExportPhase;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.exporter.FileToZipInserter;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupAuditTrailWriter;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExportData;
import com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExportWriter;
import com.bnpp.regliss.exporter.writer.format.deliveryreport.DeliveryReportExportWriter;
import com.bnpp.regliss.exporter.writer.format.dj.DJExportWriter;
import com.bnpp.regliss.exporter.writer.format.dj.DJFileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.WatchlistLogFormatWriter;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.ListsWatchlistPVLivraison;
import com.bnpp.regliss.exporter.writer.format.dj.watchlistxml.WatchlistPVXmlFormatWriter;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.FircosoftLogFormatWriter;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.FircosoftPVXmlFormatWriter;
import com.bnpp.regliss.exporter.writer.format.fircosoftxml.ListsPVLivraison;
import com.bnpp.regliss.exporter.writer.format.regular.RegularExportWriter;
import com.bnpp.regliss.exporter.writer.format.regular.RegularFileExportData;
import com.bnpp.regliss.importer.dj.DJXsdValidator;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.EmailService;
import com.bnpp.regliss.service.IAutocloseableResources;
import com.bnpp.regliss.utils.MapUtils;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupLineForSynthesis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

@Service
@ReglissBatchProfile
@Slf4j
public class FileGenerator {

    @Autowired
    private IAutocloseableResources fileHelper;

    @Autowired
    private DJXsdValidator xsdValidator;

    @Autowired
    private GeneratedFileWriterFactory writerFactory;

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private AddModSupExportWriter addModSupExportWriter;

    @Autowired
    private AddModSupAuditTrailWriter addModSupAuditTrailWriter;

    @Autowired
    private RegularExportWriter regularExportWriter;

    @Autowired
    private DJExportWriter djExportWriter;

    @Autowired
    private DeliveryReportExportWriter deliveryReportExportWriter;

    @Autowired
    private EmailService emailService;

    public Map<GeneratedFile, FileGenerationTry> tryGenerateRegularFiles(Collection<GeneratedFile> filesToGenerate,
        RegularFileExportData fileExportData, ExportJobParameters exportJobParameters) {
        Consumer<List<GeneratedFileWriter>> action = openWriters -> regularExportWriter.generateAllRegularFilesForExportMode(fileExportData,
                openWriters, exportJobParameters);
        return tryGenerateRecordsRelatedFiles(filesToGenerate, fileExportData, exportJobParameters, action);
    }

    public Map<GeneratedFile, FileGenerationTry> tryGenerateDjFiles(Collection<GeneratedFile> filesToGenerate,
        DJFileExportData fileExportData, ExportJobParameters exportJobParameters) {
        Consumer<List<GeneratedFileWriter>> action = openWriters -> djExportWriter.generateAllDjFiles(fileExportData,
                openWriters, exportJobParameters);
        return tryGenerateRecordsRelatedFiles(filesToGenerate, fileExportData, exportJobParameters, action);
    }

    public File tryGenerateAddModSupFile(AddModSupExportData addModSupExportData, ExportJobParameters exportJobParameters) {
        File file = fileHelper.createTempFileForThisThread();
        addModSupExportWriter.generateAddModSupFile(file, addModSupExportData, exportJobParameters);
        return file;
    }

    public FileGenerationTry tryGenerateZip(GeneratedFile zipToGenerate, Map<String, FileGenerationTry> zipFiles,
                                            ExportJobParameters exportJobParameters) {
        Supplier<File> action = () -> generateZip(zipToGenerate, zipFiles, exportJobParameters);
        return tryGenerateFile(zipToGenerate.getFullName(), action, zipToGenerate.getVersion());
    }

    public FileGenerationTry tryGenerateDeliveryReportFile(GeneratedFile generatedFile, Set<ReglissList> listsToBeIncluded,
                                                           ExportJobParameters exportJobParameters) {
        Supplier<File> action = () -> generateFileWithCustomWriter(generatedFile,
                generatedFileWriter -> deliveryReportExportWriter.generateDeliveryReport(listsToBeIncluded, generatedFileWriter,
                        exportJobParameters));
        return tryGenerateFile(generatedFile.getFullName(), action, generatedFile.getVersion());
    }

    public FileGenerationTry tryGenerateFircosoftPVXml(String pvFileName, ListsPVLivraison fircosoftPVLists, Version version) {
        Supplier<File> action = () -> generateFileWithDefaultWriter(writer ->
                FircosoftPVXmlFormatWriter.generateFircosoftPvXml(fircosoftPVLists, writer));
        return tryGenerateFile(pvFileName, action, version);
    }

    public FileGenerationTry tryGenerateFircosoftLog(Map<String, FileGenerationTry> fircosoftFiles, String logFileName, Version version) {
        Supplier<File> action = () -> generateFileWithDefaultWriter(writer ->
                FircosoftLogFormatWriter.generateLogFile(version, fircosoftFiles, logFileName, writer));
        return tryGenerateFile(logFileName, action, version);
    }

    public FileGenerationTry tryGenerateFircosoftLogForConcatenatedList(Map<String, FileGenerationTry> fircosoftFiles, String logFileName) {
        Supplier<File> action = () -> generateFileWithDefaultWriter(writer ->
                FircosoftLogFormatWriter.generateLogFileForConcatenatedList(fircosoftFiles, logFileName, writer));
        return tryGenerateFile(logFileName, action, null);
    }

    public FileGenerationTry tryGenerateWatchlistPVXml(String pvXmlFilename, ListsWatchlistPVLivraison watchlistPVLists, Version version) {
        Supplier<File> action = () -> generateFileWithDefaultWriter(writer ->
                WatchlistPVXmlFormatWriter.generateXmlPVFile(watchlistPVLists, writer));
        return tryGenerateFile(pvXmlFilename, action, version);
    }

    public FileGenerationTry tryGenerateWatchlistLog(DJExportFilter filter, Set<Subscription> listSubscriptions,
                                                     Map<String, FileGenerationTry> zipFiles, String logFilename, Version version) {
        Supplier<File> action = () -> generateFileWithDefaultWriter(writer ->
                WatchlistLogFormatWriter.generateLogFile(zipFiles, filter, listSubscriptions, logFilename, version, writer));
        return tryGenerateFile(logFilename, action, version);
    }

    public File tryGenerateAddModSupAuditTrailFile(Map<AddModSupAction, List<AddModSupLineForSynthesis>> dataToExport) {
        File file = fileHelper.createTempFileForThisThread();
        addModSupAuditTrailWriter.generateAddModSupAuditTrailFile(file, dataToExport);
        return file;
    }

    private Map<GeneratedFile, FileGenerationTry> tryGenerateRecordsRelatedFiles(Collection<GeneratedFile> filesToGenerate,
        FileExportData fileExportData,  ExportJobParameters exportJobParameters, Consumer<List<GeneratedFileWriter>> action) {
        Map<GeneratedFile, FileGenerationTry> generatedFiles = new HashMap<>();
        try {
            Map<GeneratedFile, File> exportModeFiles = tryGenerateFiles(filesToGenerate, fileExportData, action, exportJobParameters);

            for(Entry<GeneratedFile, File> entry : exportModeFiles.entrySet()) {
                if (entry.getKey().getGeneratedFileFormat().getFileExtension() == FileExportExtension.xml) {
                    validateXsdForDjXmlFile(entry.getValue(), entry.getKey(), exportJobParameters);
                }
            }

            generatedFiles.putAll(MapUtils.mapValues(exportModeFiles, FileGenerationTry::new));
        } catch (Exception e) {
            ReglissException failedExportModeException = new ReglissException(e, "Failed export type: " + fileExportData.getExportType());
            log.info("All File Generation for Export type Failed: {} - {}" , fileExportData.getExportType(), e.getMessage());
            log.info("All File Generation for Export type Failed ex", failedExportModeException);
            emailService.sendEmailExportError(failedExportModeException.getMessage(), fileExportData.getVersion());
            for (GeneratedFile fileToGenerate : filesToGenerate) {
                generatedFiles.put(fileToGenerate, new FileGenerationTry(failedExportModeException));
            }
        }
        return generatedFiles;
    }

    private FileGenerationTry tryGenerateFile(String fullName, Supplier<File> action, Version version) {
        try {
            log.debug("Generating file {}", fullName);
            File file = PerformanceUtil.runAndMeasure("Generation completed ", action);
            return new FileGenerationTry(file);
        } catch (Exception e) {
            log.info("File Generation Failed for file:{} with message: {} ", fullName, e.getMessage(), e);
            log.info(e.getMessage(), e);
            emailService.sendEmailExportError(e.getMessage(), version);
            return new FileGenerationTry(e);
        }
    }

    private Map<GeneratedFile, File> tryGenerateFiles(Collection<GeneratedFile> filesToGenerate, FileExportData fileExportData,
        Consumer<List<GeneratedFileWriter>> generationAction, ExportJobParameters exportJobParameters) {
        PerformanceMonitor.clearStats();
        List<GeneratedFileWriter> openWriters = new ArrayList<>();
        try {
            Map<GeneratedFile, File> resultMap = new HashMap<>();
            for (GeneratedFile fileToGenerate : filesToGenerate) {
                File file = fileHelper.createTempFileForThisThread();

                if (fileToGenerate.shouldBeGenerated(!fileExportData.hasNoRecordsToExport())) {
                    Writer writer = writerFactory.openWriter(fileToGenerate.getGeneratedFileFormat(), file); // NOSONAR: writer is closed in finally
                    GeneratedFileWriter fileWriter = writerFactory.getWriterBasedOnType(fileToGenerate.getGeneratedFileFormat(), writer);
                    openWriters.add(fileWriter);
                } else {
                    progressService.incrementBatchExportProgressForWritingAllPagesAtOnceOrNoPages(ExportPhase.EXPORT_WRITE_RECORDS, 1, exportJobParameters);
                }
                resultMap.put(fileToGenerate, file);
                GeneratedFileFormat generatedFileFormat=fileToGenerate.getGeneratedFileFormat();
                if(FileExportFormat.DJ_WATCHLIST.equals(generatedFileFormat.getFileFormat()) && FileExportExtension.xml
                        .equals(generatedFileFormat.getFileExtension())) {
                    exportJobParameters.addStatistics(fileToGenerate.getFullName(),
                            fileExportData.computeStatistics(generatedFileFormat.getFilter().getId()));
                }
            }

            generationAction.accept(openWriters);
            log.info("Generated {} files for export type {}. Formats: {}", resultMap.size(), fileExportData.getExportType(), resultMap.keySet());
            return resultMap;
        } finally {
            closeAllWriters(openWriters);
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    private void validateXsdForDjXmlFile(File tempFile, GeneratedFile fileToGenerate, ExportJobParameters exportJobParameters) {
        try {
            InputStream is = fileHelper.openInputStreamForThisThread(tempFile);//NOSONAR
            List<String> xsdErrors = xsdValidator.validateDJXml(() -> is,
                    fileToGenerate.getGeneratedFileFormat().getFileFormat().getDJFormat());
            if (!xsdErrors.isEmpty()) {
                log.debug("Xsd errors:{} ", xsdErrors);
                throw new ReglissException("Xsd validation failed for the generated file of format "
                        + fileToGenerate.getGeneratedFileFormat().getFileFormat()
                        + ". This may be caused by a change of the XML structure in the imported files.\n");
            }
            progressService.incrementExportProgressForXsdValidation(exportJobParameters);
        } catch (IOException e) {
            throw new ReglissException(e, "Could not open generated file: " + tempFile);
        }
    }

    private File generateFileWithCustomWriter(GeneratedFile generatedFile, Consumer<GeneratedFileWriter> exportFormatWriter) {
        File file = fileHelper.createTempFileForThisThread();
        try (Writer writer = writerFactory.openWriter(generatedFile.getGeneratedFileFormat(), file);
             GeneratedFileWriter generatedFileWriter = writerFactory.getWriterBasedOnType(generatedFile.getGeneratedFileFormat(), writer)) {
            exportFormatWriter.accept(generatedFileWriter);
            return file;
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    private File generateFileWithDefaultWriter(Consumer<BufferedWriter> exportFormatWriter) {
        File file = fileHelper.createTempFileForThisThread();
        try (FileWriter writer = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(writer)) {
            exportFormatWriter.accept(bw);
            return file;
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    public File generateZip(GeneratedFile zipToGenerate, Map<String, FileGenerationTry> zipFiles, ExportJobParameters exportJobParameters) {
        try {
            log.debug("Generating file {} of format {}", zipToGenerate.getFullName(), zipToGenerate.getGeneratedFileFormat().getFileFormat());

            File fileZipArchive = fileHelper.createTempFileForThisThread();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(fileZipArchive))) {
                for (Map.Entry<String, FileGenerationTry> entry : zipFiles.entrySet()) {
                    log.debug("try to add to zip {}-{}",entry.getKey(),entry.getValue().getFile());
                    if(log.isDebugEnabled() && entry.getValue().getFile()!=null){
                        log.debug("filename {}",entry.getValue().getFile().getAbsolutePath());
                    }
                    FileToZipInserter.addToZip(entry.getKey(), entry.getValue().getFile(), zipOutputStream);
                }
            }
            progressService.incrementBatchExportProgressForWritingAllPagesAtOnceOrNoPages(ExportPhase.EXPORT_WRITE_RECORDS,
                    1, exportJobParameters);
            return fileZipArchive;
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    private void closeAllWriters(List<GeneratedFileWriter> openWriters) {
        for (GeneratedFileWriter openWriter : openWriters) {
            try {
                openWriter.close();
            } catch (Exception e) {
                log.trace("Could not close a file", e);
            }
        }
    }

}
