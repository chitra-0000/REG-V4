package com.bnpp.regliss.importer.dj;

// [MISSING FROM SCREENSHOTS: lines 3-66 of the original file were not visible in the photos]

@Service
@ReglissBatchProfile
@Slf4j
public class AutomaticImportXmlService {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AutoXmlReferentialsExtractor xmlReferentialsExtractor;

    @Autowired
    private AutoDbReferentialsExtractor dbReferentialsExtractor;

    @Autowired
    private VersionReferentialsUploader versionRefUploader;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private XsltTransformerFactory xsltFactory;

    @Value("${automatic.import.page.size}")
    private int pageSize;

    @Value("${automatic.import.db.thread.pool.size:4}")
    private int updaterThreadPoolSize;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private IAutocloseableResources fileHelper;

    @Autowired
    private WatchlistAssociationParser associationParser;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BatchProgressService progressService;

    private static final String VALIDATION_BIC_LENGTH_FR ="Invalid BIC code";

    private RecordUpdateEngine newRecordUpdateEngine() { return applicationContext.getBean(RecordUpdateEngine.class); }

    @CloseResourcesAfter
    public void importAutomaticFileWithoutTx(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier,
                                             ImportFileType format, AutomaticImportFeedBase feed,
                                             long versionId, long batchExecutionId,
                                             ImportConfiguration config) {
        RecordUpdateEngine engine = newRecordUpdateEngine();
        // all xml imports should be treated as automatic so no difference is between the import of custom xml  automatic or manual
        // !feed.isAuto()  would give different comparison for aliases and different audit entries
        engine.initializeForImportIn(versionId, false);

        try {
            PerformanceMonitor.before(Action.EVERYTHING);
            if(feed.isAuto()) {
                versionRefUploader.uploadXmlReferentialsInVersionDJ(versionId, xmlInputStreamSupplier);
                log.info("Uploaded XML references into VERSION_DJ");
            }

            DJXmlReferentials xmlReferentials = xmlReferentialsExtractor.extractReferentialsUsedInParsing(xmlInputStreamSupplier, format);

            DbReferentials dbReferentials = dbReferentialsExtractor.getReferentials(format);

            IAssociationTempStorage associationStorage = selectAssociationsExtractor(format);
            associationStorage.extractToTempFile(xmlInputStreamSupplier);

            CompareXmlSnippetWithDB snippetFilter = new CompareXmlSnippetWithDB(recordRepo,
                    engine.getExternalReferencesToIdInPreviousVersion());
            RecordXmlParser recordXmlParser = new RecordXmlParser(
                    () -> xsltFactory.getTransformerByFileType(format, xmlReferentials, dbReferentials),
                    associationParser, feed);
            RecordXmlParser.removeTransformerOnLocalThread();

            Function<List<RecordCumulatedXml>, List<Record>> commonLogic = xmlSnippets -> {
                List<Record> recordsPage = recordXmlParser.parseXmlToRecords(xmlSnippets, xmlReferentials.get(DJReferentialType.RELATION_TYPE));
                engine.mergeProgramsInAdvanceInSingleThreadInTx(recordsPage);

                return recordsPage;
            };

            BufferCollector<RecordCumulatedXml> insertBuffer = new BufferCollector<>(pageSize, xmlSnippets -> {

                List<Record> recordsPage = commonLogic.apply(xmlSnippets);

                PerformanceMonitor.before(Action.INSERT_DJ_RECORDS_PLUS_TX);
                engine.insertDJRecords(recordsPage);
                engine.getBusinessDataControls().stopIfErrorThresholdExceeded();
                PerformanceMonitor.after(Action.INSERT_DJ_RECORDS_PLUS_TX);
            });

            BufferCollector<RecordCumulatedXml> updateBuffer = new BufferCollector<>(pageSize, xmlSnippets -> {

                List<Record> recordsPage = commonLogic.apply(xmlSnippets);

                PerformanceMonitor.before(Action.UPDATE_DJ_RECORDS_PLUS_TX);
                engine.updateDJRecords(recordsPage);
                engine.getBusinessDataControls().stopIfErrorThresholdExceeded();
                PerformanceMonitor.after(Action.UPDATE_DJ_RECORDS_PLUS_TX);
            });

            long totalRecords = OriginalRecordCounterSAXHandler.countRecords(xmlInputStreamSupplier);
            ProgressMonitor monitor = new ProgressMonitor("ImportXML", log, totalRecords);

            BackpressureExecutor dbExecutor = new BackpressureExecutor("autoupd", updaterThreadPoolSize, requestContext);
            try {

                log.info("Record import from XML...");
                try (XmlSnippetIterable iterable = new XmlSnippetIterable(xmlInputStreamSupplier.get(), getRecordTags(format));
                     IAssociationReader associationReader = associationStorage.openReader()) {

                    Iterable<RecordCumulatedXml> accumulatedIterator = () -> Iterators.transform(iterable,
                            recordXmlSnippet -> accumulate(recordXmlSnippet, associationReader,format));
                    for (List<RecordCumulatedXml> xmlSnippets : Iterables.partition(accumulatedIterator, pageSize)) {
                        monitor.add(xmlSnippets.size());
                        dbExecutor.blockingSubmit(() -> {
                            Map<Operation, List<RecordCumulatedXml>> xmlWithOperation = snippetFilter.compareWithDatabase(xmlSnippets,format);

                            insertBuffer.collect(xmlWithOperation.get(Operation.INSERT));
                            updateBuffer.collect(xmlWithOperation.get(Operation.UPDATE));
                        });

                        progressService.incrementImportAutoBatchExecution(batchExecutionId, ImportAutoPhase.IMPORT_AUTO_XML,
                                config, xmlSnippets.size(), totalRecords);
                    }
                }
                monitor.end();
            } finally {
                dbExecutor.shutdownAndWaitToDie();
            }

            log.debug("Finishing up: flushing the buffers");
            insertBuffer.flush();
            updateBuffer.flush();
            log.debug("Insert+update records done");


            // Phase 7 :delete records
            deleteRecords(xmlInputStreamSupplier, feed, engine, snippetFilter.getSeenExternalReferences());
            /*The whole import file was processed and the error constraint threshold was not reached
             * but the import should be stopped if there is at least one error constraint. We still want to keep
             * an error threshold (like a buffer) before failing so the file can be corrected faster */
            engine.getBusinessDataControls().stopIfAnyConstraintViolationsErrors();
            PerformanceMonitor.after(Action.EVERYTHING);
        } finally {
            PerformanceMonitor.printStats();
            PerformanceMonitor.clearStats();
            sendEmailsForConstraintViolationsAndTruncatedStrings(versionId, engine.getBusinessDataControls());
            addImportLineErrorsReportToFeed(engine.getBusinessDataControls(), feed);
        }
    }

    private void sendEmailsForConstraintViolationsAndTruncatedStrings(long versionId, BusinessDataControls businessDataControls){
        emailService.sendEmailIfConstraintViolations(versionId, businessDataControls);
        emailService.sendEmailIfTruncatedStringFields(versionId, businessDataControls);
    }

    private List<String> getRecordTags(ImportFileType format) {
        if(format.isDjFormat()) {
            return asList("Person", "Entity");
        } else {
            return singletonList("record");
        }
    }

    private IAssociationTempStorage selectAssociationsExtractor(ImportFileType format) {
        if (format == ImportFileType.DOW_JONES_WATCHLIST) {
            return new WatchlistAssociationTempStorage(fileHelper.createTempFileForThisThread("associations-temp", ".dat"));
        }

        if(format == ImportFileType.CUSTOM) {
            return new CustomImportAssociationTempStorage();
        } else {
            return new CoreAssociationTempStorage();
        }
    }

    private RecordCumulatedXml accumulate(String recordXmlSnippet, IAssociationReader associationReader, ImportFileType format) {
        String externalReference = XmlSnippetExternalReferenceExtractor.extractIdFromXml(recordXmlSnippet,format);
        String associationsXml = associationReader.readSnippet(externalReference);
        return new RecordCumulatedXml(recordXmlSnippet, associationsXml);
    }

    private void deleteRecords(Supplier<UnicodeBOMInputStream> xmlInputStreamSupplier, AutomaticImportFeedBase feed,
                               RecordUpdateEngine engine, List<String> seenExternalReferences) {
        log.info("Deleting items...");
        PerformanceMonitor.before(Action.DELETE_RECORDS);
        if (feed.isFullImport()) {
            log.debug("Computing diff ids between {} total - {} seen",
                    engine.getExternalReferencesToIdInPreviousVersion().keySet().size(),
                    seenExternalReferences.size());
            Set<String> externalReferencesToDelete = new HashSet<>(engine.getExternalReferencesToIdInPreviousVersion().keySet());
            log.debug("minus");
            HashSet<String> setToDelete = new HashSet<>(seenExternalReferences);
            // for performance, the parameter to removeAll should be a Set<>
            externalReferencesToDelete.removeAll(setToDelete);
            engine.deleteRecords(externalReferencesToDelete);
        } else {
            List<String> externalReferencesToDelete = extractExternalReferencesToDelete(xmlInputStreamSupplier,
                    new ListIdAndVersionId(engine.getListId(), engine.getVersionId()));
            engine.deleteRecords(externalReferencesToDelete);
        }
        PerformanceMonitor.after(Action.DELETE_RECORDS);
    }


// [MISSING FROM SCREENSHOTS: lines 281-283 of the original file were not visible in the photos]
            log.info("Collecting records to delete...");
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            OriginalRecordDeletedSAXHandler handler = new OriginalRecordDeletedSAXHandler();
            saxParser.parse(inputStream, handler);
            log.debug("Collected {} records to delete", handler.getExternalReferencesToDelete().size());
            return handler.getExternalReferencesToDelete();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            this.emailService.sendEmailImportError(e.getMessage(), listIdAndVersionId);
            throw new ReglissException(e);
        }
    }
    private void addImportLineErrorsReportToFeed(BusinessDataControls businessDataControls, AutomaticImportFeedBase feed){
        businessDataControls.getTruncatedEntities().forEach(elem -> feed.addToErrors(fillTruncatedEntitiesInErrors(elem)));
        feed.addToErrors(businessDataControls.getConstraintViolationsErrors().stream().map(this::fillConstraintViolationsErrorsInErrors)
                .collect(toList()));

    }

    private List<ImportLineErrorReport> fillTruncatedEntitiesInErrors(TruncatedRecordFieldsValues elem){
        return elem.getTruncateStringList().stream()
                .map(l -> new ImportLineErrorReport.ImportLineErrorReportBuilder("Data length control : field must be "
                        + l.getTruncatedValue().length() + " characters long")
                        .withExternalReference(elem.getRecordExternalReference())
                        .withToastMessage("Data length control triggered")
                        .withType(ImportLineErrorReport.ErrorType.NON_BLOCKING)
                        .withField(l.getFieldName())
                        .withValue("Field is " + l.getOriginalValue().length() +  " characters")
                        .build())
                .collect(toList());
    }

    private ImportLineErrorReport fillConstraintViolationsErrorsInErrors(ConstraintViolation<Validable> elem){
        if (elem.getMessage().equals(VALIDATION_BIC_LENGTH_FR)) {
            log.info("Invalid length, BIC code must be 8 or 11 characters for record reference {}", elem.getRootBean().getBusinessId());
            return new ImportLineErrorReport.ImportLineErrorReportBuilder(elem.getMessage())
                    .withExternalReference(elem.getRootBean().getBusinessId())
                    .withToastMessage("Invalid BIC Code Length")
                    .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                    .withField("BIC Code")
                    .withValue(elem.getInvalidValue().toString())
                    .build();
        } else {
            return new ImportLineErrorReport.ImportLineErrorReportBuilder(elem.getMessage())
                    .withExternalReference(elem.getRootBean().getBusinessId())
                    .withToastMessage("Name or Label (Program) missing : please see the report for more details")
                    .withType(ImportLineErrorReport.ErrorType.BLOCKING)
                    .build();
        }
    }


}

