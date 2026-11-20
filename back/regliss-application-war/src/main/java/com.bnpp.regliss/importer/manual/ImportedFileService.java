package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-36 of the original file were not visible in the photos]

@Slf4j
@Service
public class ImportedFileService {

    @Autowired
    private ImportedFileRepository repo;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private BlobService blobService;

    @Autowired
    private IAutocloseableResources fileHelper;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    private FileUtils fileUtils;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportedFile saveManualUploadedFileInTx(String uploadedFileName, String reglissFileName, long versionId,
                                                   UploadStatus status) {
        log.debug("creating uploaded file {} ", uploadedFileName);
        ImportedFile importedFile = new ImportedFile();
        importedFile.setUserName(requestContext.getUsername());
        importedFile.setFileName(reglissFileName);
        importedFile.setOriginalFileName(uploadedFileName);
        importedFile.setUploadDate(LocalDateTime.now());
        importedFile.setStatus(status);

        Version version = versionRepository.getExactlyOne(versionId);
        version.addImportedFile(importedFile);
        importedFile.setList(version.getList());
        importedFile.setUploadType(ImportedFile.UploadType.MANUAL);
        return repo.save(importedFile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveImportedFileInTx(File inputFile, String originalFilename, Long versionId, Long uploadedFileId) {
        Version version = versionRepository.getExactlyOne(versionId);
        ImportedFile importedFile = null;
        if(uploadedFileId == null){
            importedFile = new ImportedFile();
            importedFile.setUploadType(ImportedFile.UploadType.AUTO);
            importedFile.setOriginalFileName(originalFilename);
            importedFile.setFileName(originalFilename);
            importedFile.setList(version.getList());
            importedFile.setUserName(User.SYS_USERNAME);
            importedFile.setProcessNode(nodeDetailsSupplier.getNodeId());
            importedFile.setUploadDate(LocalDateTime.now());
        }else{
            importedFile = repo.getExactlyOne(uploadedFileId);
        }
        importedFile.setStatus(UploadStatus.IMPORTED_OK);
        version.addImportedFile(importedFile);
        version.setFileName(originalFilename);
        uploadFileContent(importedFile, inputFile, originalFilename);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImportedFileOk(Long uploadedFileId, File tempFile, String originalFileName) {
        log.debug("import ok for uploaded file {} ", uploadedFileId);
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(UploadStatus.IMPORTED_OK);
        uploadFileContent(importedFile, tempFile, originalFileName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void setProcessingInTX(long uploadedFileId) {
        log.debug("start processing for uploaded file {} ", uploadedFileId);
        repo.getExactlyOne(uploadedFileId).setStatus(ImportedFile.UploadStatus.IMPORTING);
    }

    public Optional<ImportedFile> getLastUploadedFileForVersion(Version version) {
        return repo.findTop1ByVersionOrderByUploadDateDesc(version);
    }

    public ImportedFile getByFileId(long fileId) {
        return repo.findById(fileId).orElseThrow(NoSuchElementException::new);
    }

    public void setImportedKOInTx(Long uploadedFileId, List<ImportLineErrorsDto> lineErrors, ReglissException reglissException) {
        setImportedKOInTx(uploadedFileId, new ImportErrorDto(reglissException, lineErrors));
    }

    public void setImportedKOInTx(Long uploadedFileId, List<ImportLineErrorsDto> lineErrors) {
        setImportedKOInTx(uploadedFileId, new ImportErrorDto(lineErrors));
    }

    public void setImportedKOInTx(Long uploadedFileId) {
        setImportedKOInTx(uploadedFileId, new ImportErrorDto(new ReglissException(ReglissException.ErrorCode.GENERAL)));
    }

    public void setSemiAutoImportedKOInTx(Long uploadedFileId, List<ImportLineErrorReport> errors) {
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        importedFile.setStatus(ImportedFile.UploadStatus.IMPORT_KO);
        JacksonUtil.getSerializedValue(errors)
                .ifPresent(importedFile::setErrorMessage);
        repo.save(importedFile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSemiAutoImportedOKInTx(Long uploadedFileId, List<ImportLineErrorReport> errors) {
        log.debug("update errors for uploaded file {} ", uploadedFileId);
        ImportedFile importedFile = repo.getExactlyOne(uploadedFileId);
        JacksonUtil.getSerializedValue(errors)
                .ifPresent(importedFile::setErrorMessage);
    }

    private void uploadFileContent(ImportedFile importedFile, File inputFile, String originalFilename) {
        log.debug("start uploading import file {} with id {} ", originalFilename, importedFile.getId());
