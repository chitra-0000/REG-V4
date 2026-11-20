package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.importer.ManualImportStats;
import com.bnpp.regliss.importer.RecordUpdateEngine;
import com.bnpp.regliss.monitoring.PerformanceMonitor;
import com.bnpp.regliss.monitoring.ProgressMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.MappingFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.VersionAuditService;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.service.VersionService;
import com.google.common.collect.Iterables;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ObjectUtils;
import org.apache.poi.hssf.eventusermodel.HSSFUserException;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bnpp.regliss.entity.UploadedFileExtension.XLS;
import static java.util.Collections.emptyList;

@Slf4j
@Component
@Scope("prototype")
@ReglissBatchProfile
public class ManualImportPagedJob {
    public static final List<LineImportErrors> NO_ERRORS = emptyList();
    @Autowired
    private VersionRepository versionRepo;
    @Autowired
    private VersionAuditService versionAuditService;
    @Autowired
    private MappingFileRepository mappingFileRepository;
    @Autowired
    private RecordParser recordParser; // @Autowired another prototype
    @Autowired
    private RecordUpdateEngine engine; // @Autowired another prototype
    @Autowired
    private VersionService versionService;
    @Autowired
    private VersionLockService versionLockService;
    @Autowired
    private ReferenceService referenceService;
    @Autowired
    private BatchProgressService progressService;


    private ManualImportStats stats = new ManualImportStats(); // NOSONAR: data field

    public static final int PAGE_SIZE = 100;

    private TransactionTemplate tx;

    private MappingFile mapping;
    private ExternalReferenceRegistry registry;
    @Getter
    private Long versionId;

    private long batchExecutionId;

    @Autowired
    public void setDataSource(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<LineImportErrors> launchJob(long versionId, File file, ImportedFile uploadedFile, long batchExecutionId) {
        this.versionId = versionId;
        this.batchExecutionId=batchExecutionId;
        if (uploadedFile.getExtension() == XLS) {
            return launchXLS(file, uploadedFile.getOriginalFileName());
        } else {
            return launchCsv(file, uploadedFile.getOriginalFileName());
        }
    }

    public List<LineImportErrors> launchCsv(File file, String originalFileName) {
        try (CsvReader csvReader = new CsvReader(new FileInputStream(file));
                CsvReader csvReader4LineNr = new CsvReader(new FileInputStream(file))) {
            PerformanceMonitor.before(PerformanceMonitor.Action.EVERYTHING);

            Version version = versionRepo.getExactlyOne(versionId);
            validateVersionStatus(version);

            mapping = mappingFileRepository.findActiveByListId(version.getList().getId())
                    .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.MAPPING_FILE_NOT_DEFINED));

            if (!csvReader.hasNext()) {
                throw new IllegalArgumentException("No header line");
            }

            ParsedLine headerLine = csvReader.next();
            validateHeader(headerLine);

            int totalRecordsNumber = csvReader4LineNr.nrOfLines() - 1;
            List<LineImportErrors> errors = importRecordData(versionId, csvReader, totalRecordsNumber);

            if (errors.isEmpty()) {
                log.debug("Start computing version statistics {}",versionId);
                versionService.writeCountRecordNumbers(versionId);
                log.debug("Statistics saved for version {}",versionId);
                completeImportInTx(versionId, originalFileName);
                PerformanceMonitor.after(PerformanceMonitor.Action.EVERYTHING);
            }

            return errors;
        } catch (IOException e) {
            throw new ReglissException(e, "Could not read file: " + file.getAbsolutePath());
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    private List<LineImportErrors> importRecordData(long versionId, FlatDataSource flatDataSource, int totalRecordsNumber) {
        registry = new ExternalReferenceRegistry(mapping);
        List<LineImportErrors> errors = new ArrayList<>();
        Iterable<ParsedLine> linesIterable = () -> flatDataSource;

        initialize(versionId);

        ProgressMonitor progress = new ProgressMonitor("ImportManual", log, 100_000);

        for (List<ParsedLine> page : Iterables.partition(linesIterable, PAGE_SIZE)) {
            try{
                versionLockService.checkAndExtendAutomaticImportLock(versionId);
            }catch(ReglissException e){
                log.error("lost lock on automatic import",e);
                throw new ReglissException("Version id " + versionId + " is missing SYS lock, abort import!");
            }

            if (importRecordPageData(page, progress, errors, registry).size() >= 20) {
                break;
            }

            progressService.incrementImportSemiAutoBatchExecution(batchExecutionId, page.size(), totalRecordsNumber);
        }
        progress.end();

        PerformanceMonitor.before(PerformanceMonitor.Action.DELETE_RECORDS);
        List<String> externalReferencesToDelete = new ArrayList<>(
                engine.getExternalReferencesToIdInPreviousVersion().keySet());
        externalReferencesToDelete.removeAll(registry.getSeenExternalReferences());
        stats.setTotalDeleted(externalReferencesToDelete.size());
        engine.deleteRecords(externalReferencesToDelete);
        PerformanceMonitor.after(PerformanceMonitor.Action.DELETE_RECORDS);
        log.debug("Totals: {}", stats);
        return errors;
    }

    public List<LineImportErrors> launchXLS(File file, String originalFileName) {
        try (FileInputStream fis = new FileInputStream(file); HSSFWorkbook myWorkBook = new HSSFWorkbook(fis)) {
            PerformanceMonitor.before(PerformanceMonitor.Action.EVERYTHING);


            Version version = versionRepo.getExactlyOne(versionId);
            validateVersionStatus(version);

            mapping = mappingFileRepository.findActiveByListId(version.getList().getId())
                    .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.MAPPING_FILE_NOT_DEFINED));

            registry = new ExternalReferenceRegistry(mapping);
            initialize(versionId);
            ProgressMonitor progress = new ProgressMonitor("ImportManual", log, 100_000);

            HSSFSheet mySheet = myWorkBook.getSheetAt(0);
            int totalRecordsNumber = mySheet.getLastRowNum() + 1;
            XLSStreamParser xlsStreamParser = new XLSStreamParser(file, this, totalRecordsNumber, progress);
            xlsStreamParserProcess(xlsStreamParser);

            if (!xlsStreamParser.getParsedLines().isEmpty()) {
                xlsStreamParser.getErrors().addAll(importRecordData(xlsStreamParser.getParsedLines(), xlsStreamParser.getErrors(),
                        progress, totalRecordsNumber));
                xlsStreamParser.getParsedLines().clear();
            }
            if (!xlsStreamParser.getErrors().isEmpty()) {
                return xlsStreamParser.getErrors();
            }
            progress.end();

            PerformanceMonitor.before(PerformanceMonitor.Action.DELETE_RECORDS);
            List<String> externalReferencesToDelete = new ArrayList<>(
                    engine.getExternalReferencesToIdInPreviousVersion().keySet());
            externalReferencesToDelete.removeAll(registry.getSeenExternalReferences());
            stats.setTotalDeleted(externalReferencesToDelete.size());
            engine.deleteRecords(externalReferencesToDelete);
            PerformanceMonitor.after(PerformanceMonitor.Action.DELETE_RECORDS);
            log.debug("Totals: {}", stats);

            log.debug("Start computing version statistics {}",versionId);
            versionService.writeCountRecordNumbers(versionId);
            log.debug("Statistics saved for version {}",versionId);

            completeImportInTx(versionId, originalFileName);
            PerformanceMonitor.after(PerformanceMonitor.Action.EVERYTHING);
            return NO_ERRORS;
        } catch (IOException e) {
            throw new ReglissException(e, "Could not read file: " + file.getAbsolutePath());
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    private void xlsStreamParserProcess(XLSStreamParser xlsStreamParser) throws IOException {
        try {
            xlsStreamParser.process();
        } catch (ReglissException e) {
            if (e.getCause() instanceof HSSFUserException) {
                log.debug("Streaming proccessing stopped due to: " + e.getCause());
            } else {
                throw e;
            }
        }
    }

    public List<LineImportErrors> importRecordData(List<ParsedLine> page, List<LineImportErrors> errors,
                                                   ProgressMonitor progress, int totalRecordsNumber) {
        try{
            versionLockService.checkAndExtendAutomaticImportLock(versionId);
        }catch(ReglissException e){
            log.error("lost lock on automatic import",e);
            throw new ReglissException("Version id " + versionId + " is missing SYS lock, abort import!");
        }
        List<LineImportErrors> importRecordPageData = importRecordPageData(page, progress, errors, registry);
        progressService.incrementImportSemiAutoBatchExecution(batchExecutionId, page.size(), totalRecordsNumber);
        return importRecordPageData;
    }

    private void initialize(long versionId) {
        engine.initializeForImportIn(versionId, true);
        recordParser.initialize(mapping);
    }

    private List<LineImportErrors> importRecordPageData(List<ParsedLine> page, ProgressMonitor progress,
            List<LineImportErrors> errors, ExternalReferenceRegistry registry) {
        if (errors.size() >= 20) {
            return errors;
        }
        PerformanceMonitor.before(PerformanceMonitor.Action.RECORD_CHECKS);
        page = checkColumnCount(page, errors);
        page = checkDuplicateExtRef(page, registry, errors);
        PerformanceMonitor.after(PerformanceMonitor.Action.RECORD_CHECKS);

        PerformanceMonitor.before(PerformanceMonitor.Action.CREATE_RECORD);
        List<Record> recordPage = createRecords(page, errors);
        PerformanceMonitor.after(PerformanceMonitor.Action.CREATE_RECORD);

        PerformanceMonitor.before(PerformanceMonitor.Action.MERGE_PROGRAMS);
        engine.mergeProgramsInAdvanceInSingleThreadInTx(recordPage);
        PerformanceMonitor.after(PerformanceMonitor.Action.MERGE_PROGRAMS);

        PerformanceMonitor.before(PerformanceMonitor.Action.INSERT_UPDATE_RECORDS);
        engine.insertOrUpdateForManualImportInTx(recordPage, stats);
        PerformanceMonitor.after(PerformanceMonitor.Action.INSERT_UPDATE_RECORDS);
        progress.add(page.size());

        return errors;
    }

    private List<Record> createRecords(List<ParsedLine> page, List<LineImportErrors> errors) {
        List<Record> records = new ArrayList<>();
        for (ParsedLine line : page) {
            RecordTry<Record> recordTry = recordParser.createRecord(mapping, line);
            if (recordTry.hasErrors()) {
                errors.add(recordTry.getErrors());
            } else {
                records.add(recordTry.getValue());
            }
        }
        return records;
    }

    private List<ParsedLine> checkDuplicateExtRef(List<ParsedLine> lines, ExternalReferenceRegistry registry,
            List<LineImportErrors> errors) {
        List<ParsedLine> okLines = new ArrayList<>();
        for (ParsedLine line : lines) {
            Optional<LineImportErrors> optError = registry.addAndTestIfNew(line);
            if (optError.isPresent()) {
                errors.add(optError.get());
            } else {
                okLines.add(line);
            }
        }
        return okLines;
    }

    private List<ParsedLine> checkColumnCount(List<ParsedLine> lines, List<LineImportErrors> errors) {
        List<ParsedLine> okLines = new ArrayList<>();
        for (ParsedLine line : lines) {
            if (line.cells.size() != mapping.getColumns().size()) {
                ValidationError error = new ValidationError(
                        ReglissException.ErrorCode.IMPORT_ROW_LENGTH_DIFFERENT_FROM_HEADER_LENGTH);
                errors.add(new LineImportErrors(line.lineNumber, error));
            } else {
                okLines.add(line);
            }
        }
        return okLines;
    }

    private void completeImportInTx(long versionId, String originalFileName) {
        tx.execute(unused -> {
            Version version = versionRepo.getExactlyOne(versionId);
            version.setFileName(originalFileName);
            versionAuditService.auditImportFile(version);
            return 1;
        });
    }

    public void validateHeader(ParsedLine headerRow) {
        if (mapping.getColumns().size() != headerRow.cells.size()) {
            throw new ReglissException(
                    ReglissException.ErrorCode.IMPORT_FILE_HEADER_LENGTH_DIFFERENT_FROM_MAPPING_FILE_LENGTH);
        }
        for (int i = 0; i < mapping.getColumns().size(); i++) {
            String expectedLabel = mapping.getColumns().get(i).getColumnLabel();
            String actualLabel = headerRow.cells.get(i);
            if (!ObjectUtils.equals(expectedLabel, actualLabel)) {
                throw new ReglissException(ReglissException.ErrorCode.IMPORT_FILE_HEADER_DIFFERENT, (i + 1) + "",
                        expectedLabel != null ? expectedLabel : "\"\"", actualLabel != null ? actualLabel : "\"\"");
            }
        }
    }

    private void validateVersionStatus(Version version) {
        if (!version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED)) {
            throw new IllegalArgumentException();
        }
    }

}
