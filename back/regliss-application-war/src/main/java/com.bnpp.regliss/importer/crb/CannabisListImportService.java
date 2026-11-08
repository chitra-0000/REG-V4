package com.bnpp.regliss.importer.crb;

// [MISSING FROM SCREENSHOTS: lines 3-32 of the original file were not visible in the photos]

@Service
@ReglissBatchProfile
@Slf4j
public class CannabisListImportService {

    @Autowired
    RecordRepository recordRepo;

    @Autowired
    ReferenceService referenceService;

    @Autowired
    ReglissRequestContext requestContext;

    @Autowired
    ObjectFactory<ManualImportPagedJob> pagedJobFactory;

    @Autowired
    SemiAutomaticCRBImportBulkJob xlsxBulkJob;

    @Autowired
    MessageSource messageSource;

    @Autowired
    VersionService versionService;

    @Autowired
    ImportedFileService importedFileService;

    @Autowired
    NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    BatchManagementService batchManagementService;

    @Autowired
    ImportedFileRepository repo;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UnblockImportService unblockImportService;

    @Autowired
    ManualImportFileRepository fileRepository;

    @Value("${version.delete.timeout.seconds:500}")
    private int versionDeletetimeoutSeconds;

    public void processUploadedFile(ImportedFile importedFile) {
        requestContext.setCurrentUser(referenceService.getSysUser());
        Long batchExecutionId = null;
        Version version = importedFile.getVersion();
        Long versionId = version.getId();

        try {
            log.debug("Start processing manual imported file for CRB list :{}", importedFile.getId());
            batchExecutionId = batchManagementService.createNewSemiAutoBatchExecutionInTx(importedFile, version);
            File tempFile = fileRepository.getFileInUploadFolderByName(importedFile.getFileName());

            if (tempFile.exists() && tempFile.isFile()) {
                List<ImportLineErrorsDto> errors = new ArrayList<>();
                try {
                    importFile(importedFile, tempFile, errors, versionId, batchExecutionId);
                } catch (ReglissException e) {
                    log.error("{} : {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "-");
                    unblockImportService.tryRevertSemiAutoVersionInTx(versionId, versionDeletetimeoutSeconds * 1000);
                    importedFileService.setImportedKOInTx(importedFile.getId(), errors, e);
                } catch (Exception e) {
                    log.error("{} : {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "-");
                    unblockImportService.tryRevertSemiAutoVersionInTx(versionId, versionDeletetimeoutSeconds * 1000);
                    importedFileService.setImportedKOInTx(importedFile.getId(), errors, new ReglissException(e));
                }
            } else {
                log.error("Import file not found {}", importedFile.getFileName());
                importedFileService.setImportedKOInTx(importedFile.getId(), singletonList(new ImportLineErrorsDto(-1,
                        singletonList("File Not Found"))));
            }
        } finally {
            unblockImportService.finishImportExecutionByRequesterUser(batchExecutionId);
            unblockImportService.unblockSemiAutoImportRegardlessTheStatusInTx(batchExecutionId, importedFile.getId());
        }
    }

    @CloseResourcesAfter
    private void importFile(ImportedFile importedFileInfo, File tempFile, List<ImportLineErrorsDto> errors, Long versionId,
                            long batchExecutionId) {
        if (XLSX.equals(importedFileInfo.getExtension())) {
            errors.addAll(translateErrors(xlsxBulkJob.allInMemoryImplement(versionId, tempFile, importedFileInfo, batchExecutionId)));
            if (errors.isEmpty()) {
                batchManagementService.updateBatchExecutionCompletedProgress(batchExecutionId);
                importedFileService.updateImportedFileOk(importedFileInfo.getId(), tempFile, importedFileInfo.getOriginalFileName());
            } else {
                // TO DO send error mail ? for manual import
                importedFileService.setImportedKOInTx(importedFileInfo.getId(), errors);
                versionService.revertVersionInTx(versionId);
            }
        }
    }

    private List<ImportLineErrorsDto> translateErrors(List<LineImportErrors> errors) {
        return errors.stream().map(this::translate).collect(toList());
    }

    private String translateError(ValidationError error) {
        return messageSource.getMessage("error." + error.getErrorCode(), error.getParameters(), Locale.ENGLISH);
    }

    private ImportLineErrorsDto translate(LineImportErrors lineErrors) {
        return new ImportLineErrorsDto(lineErrors.getLineNumber(),
                lineErrors.getErrors().stream().map(this::translateError).collect(toList()));
    }
}

