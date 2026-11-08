package com.bnpp.regliss.importer.crb;

// [MISSING FROM SCREENSHOTS: lines 3-39 of the original file were not visible in the photos]

@Slf4j
@Service
@ReglissBatchProfile
public class SemiAutomaticCRBImportBulkJob {
    public static final String COULD_NOT_READ_FROM_TEMP_FILE = "Could not read from temp file";
    private static final int PAGE_SIZE = 100;

    @Autowired
    VersionAuditService versionAuditService;

    @Autowired
    VersionRepository versionRepo;

    @Autowired
    ObjectFactory<RecordUpdateEngine> updateEngineFactory;

    @Autowired
    BatchProgressService progressService;

    @Autowired
    VersionService versionService;

    private TransactionTemplate tx; // NOSONAR : injected via setter

    @Autowired
    public void setDataSource(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<LineImportErrors> allInMemoryImplement(long versionId, File tempFile, ImportedFile importedFile, long batchExecutionId) {
        try {
            PerformanceMonitor.before(PerformanceMonitor.Action.EVERYTHING);
            return doAllInMemoryImplement(versionId, tempFile, importedFile, batchExecutionId);
        } finally {
            PerformanceMonitor.after(PerformanceMonitor.Action.EVERYTHING);
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
        }
    }

    private List<LineImportErrors> doAllInMemoryImplement(long versionId, File tempFile, ImportedFile importedFile, long batchExecutionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        validateVersionStatus(version);
        List<RecordTry<ParsedLine>> tryLines;
        int totalRecordsNumber;
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            if (importedFile == null) {
                throw new ReglissException(COULD_NOT_READ_FROM_TEMP_FILE);
            }
            totalRecordsNumber = getTotalRecordsNumber(tempFile);
            tryLines = parseFile(importedFile.getOriginalFileName(), fis);
        } catch (IOException e) {
            throw new ReglissException(e, COULD_NOT_READ_FROM_TEMP_FILE);
        }

        List<LineImportErrors> errors = new ArrayList<>();
        ExternalReferenceRegistry detector = new ExternalReferenceRegistry(CRBRecordResolver.SECURITIES_ID_INDEX);
        tryLines.stream().filter(RecordTry::isOk).map(RecordTry::getValue).forEach(detector::addAndTestIfNew);
        errors.addAll(detector.getErrors());

        List<RecordTry<Record>> tryRecords = tryLines
                .stream()
                .filter(RecordTry::isOk)
                .map(line -> CRBRecordResolver.createRecord(line.getValue()))
                .collect(Collectors.toList());

        errors.addAll(getErrors(tryLines, tryRecords));
        log.debug("Created + validated OK");

        if (errors.isEmpty()) {
            processAll(tryRecords.stream().map(RecordTry::getValue), versionId, totalRecordsNumber, batchExecutionId);
            log.debug("Start computing version statistics {}",versionId);
            versionService.writeCountRecordNumbers(versionId);
            log.debug("Statistics saved for version {}",versionId);
            completeImportInTx(versionId, importedFile.getOriginalFileName());
        }
        return errors;
    }

    private int getTotalRecordsNumber(File tempFile) {
        try (FileInputStream fis4NrLines = new FileInputStream(tempFile);
             XSSFWorkbook myWorkBook = new XSSFWorkbook(fis4NrLines)) {
            XSSFSheet mySheet = myWorkBook.getSheetAt(0);
            return mySheet.getLastRowNum() + 1;
        } catch (IOException e) {
            throw new ReglissException(e, COULD_NOT_READ_FROM_TEMP_FILE);
        }
    }

    public void processAll(Stream<Record> recordsStream, long versionId, int totalNumberOfRecords, long batchExecutionId) {
        RecordUpdateEngine engine = updateEngineFactory.getObject();
        engine.initializeForImportIn(versionId, true);
        List<String> seenExternalReferences = new ArrayList<>();

        ManualImportStats stats = new ManualImportStats();
        LambdaUtil.partition(recordsStream, PAGE_SIZE).forEach(partition -> {
            seenExternalReferences.addAll(partition.stream().map(Record::getExternalReference).collect(toList()));
            engine.mergeProgramsInAdvanceInSingleThreadInTx(partition);
            engine.insertOrUpdateForManualImportInTx(partition, stats);
            progressService.incrementImportSemiAutoBatchExecution(batchExecutionId, partition.size(), totalNumberOfRecords);
        });

        List<String> externalReferencesToDelete = new ArrayList<>(engine.getExternalReferencesToIdInPreviousVersion().keySet());
        externalReferencesToDelete.removeAll(seenExternalReferences);
        stats.setTotalDeleted(externalReferencesToDelete.size());
        engine.deleteRecords(externalReferencesToDelete);

        log.debug("TOTALS: {}", stats);
    }

    private void validateVersionStatus(Version version) {
        if (!version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED)) {
            throw new IllegalArgumentException();
        }
    }

    public List<RecordTry<ParsedLine>> parseFile(String fileName, InputStream inputStream) {
        List<ParsedLine> rows = readFile(fileName, inputStream);
        int headerSize = rows.get(0).cells.size();
        ParsedLine headerRow = rows.remove(0);
        validateHeader(headerRow);
        return checkRowsColumnCount(rows, headerSize);
    }

    private List<ParsedLine> readFile(String fileName, InputStream inputStream) {
        try {
            if (UploadedFileExtension.fromFileName(fileName) != XLSX) {
                throw new IllegalArgumentException("Only xlsx is processed in bulk mode");
            }
            return XlsxParser.readFile(inputStream);
        } catch (IOException e) {
            throw new ReglissException(e, ReglissException.ErrorCode.IMPORT_FILE_PARSE_ERROR);
        }
    }

    private void completeImportInTx(long versionId, String originalFileName) {
        tx.execute(unused -> {
            Version version = versionRepo.getExactlyOne(versionId);
            version.setFileName(originalFileName);
            versionAuditService.auditImportFile(version);
            return 1;
        });
    }

    private List<LineImportErrors> getErrors(List<RecordTry<ParsedLine>> tryLines, List<RecordTry<Record>> tryRecords) {
        List<LineImportErrors> errors = new ArrayList<>();
        errors.addAll(RecordTry.extractErrors(tryLines));
        errors.addAll(RecordTry.extractErrors(tryRecords));
        if (errors.size() > 19) {
            errors = errors.subList(0, 20);
        }
        return errors;
    }

    private void validateHeader(ParsedLine headerRow) {
        if (CRBRecordResolver.headerCells.size() != headerRow.cells.size()) {
            throw new ReglissException(ReglissException.ErrorCode.IMPORT_FILE_HEADER_LENGTH_DIFFERENT_FROM_MAPPING_FILE_LENGTH);
        }
        for (int i = 0; i < CRBRecordResolver.headerCells.size(); i++) {
            String expectedLabel = CRBRecordResolver.headerCells.get(i);
            String actualLabel = headerRow.cells.get(i);
            if (Strings.isEmpty(actualLabel) || !expectedLabel.equalsIgnoreCase(actualLabel)) {
                throw new ReglissException(ReglissException.ErrorCode.IMPORT_FILE_HEADER_DIFFERENT, (i + 1) + "",
                        expectedLabel != null ? expectedLabel : "", actualLabel != null ? actualLabel : "");
            }
        }
    }

    private List<RecordTry<ParsedLine>> checkRowsColumnCount(List<ParsedLine> rows, int headerSize) {
        return rows.stream().map(line -> tryCheckLineCount(line, headerSize)).collect(toList());
    }

    private RecordTry<ParsedLine> tryCheckLineCount(ParsedLine parsedLine, int headerSize) {
        if (parsedLine.cells.size() != headerSize) {
            ValidationError error = new ValidationError(
                    ReglissException.ErrorCode.IMPORT_ROW_LENGTH_DIFFERENT_FROM_HEADER_LENGTH);
            return new RecordTry<>(new LineImportErrors(parsedLine.lineNumber, error));
        } else {
            return new RecordTry<>(parsedLine);
        }
    }

}

