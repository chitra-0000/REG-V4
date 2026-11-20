package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.annotation.ReglissBatchProfile;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.importer.manual.validation.MappingFileValidator;
import com.bnpp.regliss.importer.manual.validation.RecordValidator;
import com.bnpp.regliss.repository.MappingFileRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.VersionAuditService;
import com.bnpp.regliss.service.VersionService;
import com.bnpp.regliss.service.progress.ProgressService;
import com.bnpp.regliss.utils.LambdaUtil;
import com.bnpp.regliss.utils.PerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@ReglissBatchProfile
public class ManualImportBulkJob {

    private static final String COULD_NOT_READ_FROM_TEMP_FILE = "Could not read from temp file";
    private static final int PAGE_SIZE = 100;

    @Autowired
    private VersionAuditService versionAuditService;

    @Autowired
    private RecordParserFactory recordParserFactory;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private UpdateEngineFactory updateEngineFactory;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private MappingFileRepository mappingFileRepository;

    @Autowired
    private VersionService versionService;

    private TransactionTemplate tx; // NOSONAR

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.tx = new TransactionTemplate();
        // TODO: tx configuration from dataSource - exact setup not fully visible
    }

    public void allInMemoryImplem(Long versionId, InputStream inputStream, String fileName) {
        PerformanceMonitor.start("allInMemoryImplem");
        doAllInMemoryImplem(versionId, inputStream, fileName);
        PerformanceMonitor.stop("allInMemoryImplem");
    }

    private void doAllInMemoryImplem(Long versionId, InputStream inputStream, String fileName) {
        Version version = versionRepo.getExactlyOne(versionId);
        versionService.validateVersionStatus(version);

        MappingFile mappingFile = mappingFileRepository.findByVersion(version);

        List<ParsedLine> parsedLines = parseFile(inputStream, mappingFile, fileName);

        ExternalReferenceRegistry externalReferenceRegistry = new ExternalReferenceRegistry(mappingFile);

        RecordValidator recordValidator = ((MappingFileValidator) mappingFile).createRecordValidator();

        List<LineImportErrors> allErrors = new ArrayList<>();
        for (ParsedLine line : parsedLines) {
            externalReferenceRegistry.addAndTestIfNew(line);
            List<ValidationError> recordErrors = recordValidator.validateRecord(mappingFile, line.cells);
            if (!recordErrors.isEmpty()) {
                allErrors.add(new LineImportErrors(line.lineNumber, recordErrors));
            }
        }

        processAll(version, parsedLines, allErrors, externalReferenceRegistry);
    }

    private void processAll(Version version, List<ParsedLine> parsedLines,
                            List<LineImportErrors> allErrors,
                            ExternalReferenceRegistry externalReferenceRegistry) {
        List<List<ParsedLine>> partitions = LambdaUtil.partition(parsedLines, PAGE_SIZE);

        for (List<ParsedLine> partition : partitions) {
            mergeProgramsInAdvanceInSingleThreadInTx(version, partition);
            insertOrUpdateForManualImportInTx(version, partition);
            incrementImportSemiAutoBatchExecution(version);
        }

        Set<String> externalReferencesToDelete = externalReferenceRegistry.getSeenExternalReferences();
        // TODO: externalReferencesToDelete logic - exact handling not fully visible
    }

    public int getTotalRecordsNumber(InputStream inputStream) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            return workbook.getSheetAt(0).getLastRowNum();
        } catch (IOException e) {
            throw new ReglissException(COULD_NOT_READ_FROM_TEMP_FILE, e);
        }
    }

    private List<ParsedLine> parseFile(InputStream inputStream, MappingFile mappingFile, String fileName) {
        List<ParsedLine> lines = readFile(inputStream, fileName);

        // remove header
        ParsedLine header = lines.remove(0);

        // FGAT RUJ00257
        validateHeader(mappingFile, header);
        checkRowsColumnCount(lines, mappingFile);

        return lines;
    }

    private List<ParsedLine> readFile(InputStream inputStream, String fileName) {
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            throw new ReglissException(ReglissException.ErrorCode.IMPORT_INVALID_FILE_FORMAT);
        }
        return XlsxParser.readFile(inputStream);
    }

    public void completeImportInTx(Long versionId, String fileName) {
        tx.execute(status -> {
            Version version = versionRepo.getExactlyOne(versionId);
            version.setFileName(fileName);
            versionAuditService.auditImportFile(version);
            return null;
        });
    }

    public List<LineImportErrors> getErrors(List<LineImportErrors> allErrors) {
        return allErrors.stream()
                .limit(20)
                .collect(Collectors.toList());
    }

    private void validateHeader(MappingFile mappingFile, ParsedLine header) {
        if (mappingFile.getColumns().size() != header.cells.size()) {
            throw new ReglissException(ReglissException.ErrorCode.IMPORT_INVALID_COLUMN_COUNT);
        }
        for (int i = 0; i < mappingFile.getColumns().size(); i++) {
            String expectedLabel = mappingFile.getColumns().get(i).getLabel();
            String actualLabel = header.cells.get(i);
            if (!expectedLabel.equals(actualLabel)) {
                throw new ReglissException(ReglissException.ErrorCode.IMPORT_INVALID_COLUMN_LABEL,
                        expectedLabel, actualLabel);
            }
        }
    }

    private void checkRowsColumnCount(List<ParsedLine> lines, MappingFile mappingFile) {
        int expectedColumnCount = mappingFile.getColumns().size();
        for (ParsedLine line : lines) {
            if (line.cells.size() != expectedColumnCount) {
                throw new ReglissException(ReglissException.ErrorCode.IMPORT_INVALID_ROW_COLUMN_COUNT,
                        String.valueOf(line.lineNumber));
            }
        }
    }

    private void tryCheckLineCount(List<ParsedLine> lines) {
        // TODO: exact line count check logic not fully visible
    }

    private void mergeProgramsInAdvanceInSingleThreadInTx(Version version, List<ParsedLine> partition) {
        // TODO: exact implementation not visible in photos
    }

    private void insertOrUpdateForManualImportInTx(Version version, List<ParsedLine> partition) {
        // TODO: exact implementation not visible in photos
    }

    private void incrementImportSemiAutoBatchExecution(Version version) {
        // TODO: exact implementation not visible in photos
    }
}
