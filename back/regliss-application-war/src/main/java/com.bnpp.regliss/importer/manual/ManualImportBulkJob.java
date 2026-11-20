package com.bnpp.regliss.importer.manual;
// [MISSING FROM SCREENSHOTS: lines 2-106 of the original file were not visible in the photos]
        ExternalReferenceRegistry detector = new ExternalReferenceRegistry(mapping);
        tryLines.stream().filter(RecordTry::isOk).map(RecordTry::getValue).forEach(detector::addAndTestIfNew);
        errors.addAll(detector.getErrors());

        log.debug("Creating+validating all records");
        RecordParser recordParser = recordParserFactory.getObject();
        recordParser.initialize(mapping);
        List<RecordTry<Record>> tryRecords = tryLines.stream().filter(RecordTry::isOk)
                .map(row -> recordParser.createRecord(mapping, row.getValue())).collect(toList());
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

        List<String> externalReferencesToDelete = new ArrayList<>(
                engine.getExternalReferencesToIdInPreviousVersion().keySet());
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

    public List<RecordTry<ParsedLine>> parseFile(String fileName, InputStream inputStream, MappingFile mapping) {
        List<ParsedLine> rows = readFile(fileName, inputStream);

        int headerSize = rows.get(0).cells.size();
        ParsedLine headerRow = rows.remove(0);
        // FGAT RUJ00257 - False Positive: MappingFile comes from the database. No need
        // for canonicalization.
        validateHeader(mapping, headerRow);

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

    private void validateHeader(MappingFile mapping, ParsedLine headerRow) {
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

