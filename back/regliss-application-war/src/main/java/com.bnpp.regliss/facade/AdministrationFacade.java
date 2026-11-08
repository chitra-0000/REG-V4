package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.batch.service.BatchJobExecutionService;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.facade.dto.BatchJobExecutionDto;
import com.bnpp.regliss.facade.dto.SingleValue;
import com.bnpp.regliss.facade.dto.administration.SubstitutionMatrixElementDetailsDto;
import com.bnpp.regliss.facade.dto.administration.SubstitutionMatrixElementDto;
import com.bnpp.regliss.facade.dto.administration.UserGuideDto;
import com.bnpp.regliss.facade.mapper.UserGuideMapper;
import com.bnpp.regliss.importer.manual.ImportedFileService;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.repository.SubstitutionMatrixElementRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.scheduler.AutomaticImportFileRepository;
import com.bnpp.regliss.scheduler.AutomaticSixImportFileRepository;
import com.bnpp.regliss.scheduler.UnblockImportService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.SubstitutionMatrixAuditService;
import com.bnpp.regliss.service.verification.FileTypeVerificationService;
import com.bnpp.regliss.utils.BlobService;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.vo.BatchExportLoggedParameters;
import com.bnpp.regliss.vo.BatchImportAutoLoggedParameters;
import com.bnpp.regliss.vo.BatchImportSemiAutoLoggedParameters;
import com.bnpp.regliss.vo.BatchJobParameterLoggable;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.web.multipart.MultipartFile;
import com.bnpp.regliss.service.pdf.UserGuideService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Facade
@ReglissWebProfile
public class AdministrationFacade {

    @Autowired
    private SubstitutionMatrixElementRepository substitutionMatrixRepo;

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepo;

    @Autowired
    private SubstitutionMatrixAuditService auditService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private BatchJobExecutionService batchJobExecutionService;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepo;

    @Autowired
    private ImportedFileService importedFileService;

    @Autowired
    private UnblockImportService unblockImportService;

    @Autowired
    private AutomaticImportFileRepository djFileRepo;

    @Autowired
    private AutomaticSixImportFileRepository sixFileRepo;

    @Autowired
    private UserGuideService userGuideService;

    @Autowired
    private BlobService blobService;

    @Autowired
    private UserGuideMapper userGuideMapper;

    @Autowired
    private FileTypeVerificationService fileTypeVerificationService;

    @Value("${batchjob.inactive.delta.seconds}")
    private long jobThreshold;

    @Value("${heartbeat.inactive.delta.seconds}")
    private long heartbeatThreshold;

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime heartbeatCutoffTime = now.minusSeconds(heartbeatThreshold);
    LocalDateTime jobCutoffTime = now.minusSeconds(jobThreshold);

    private static final Integer MINUS_MONTH_BATCH_JOBS = 3;

    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public void crudSubstitutionMatrix(List<SubstitutionMatrixElementDto> matrixElements, FileExportFormat fileFormat) {
        List<SubstitutionMatrixElement> newMatrixElements = matrixElements.stream().map(dto -> toSubstitutionMatrixElement(dto, fileFormat))
                .collect(toList());
        List<SubstitutionMatrixElement> oldMatrixElements = substitutionMatrixRepo.findByFileFormat(fileFormat);

        substitutionMatrixRepo.deleteByFileFormat(fileFormat);
        substitutionMatrixRepo.saveAll(newMatrixElements);

        auditService.audit(oldMatrixElements, newMatrixElements);

    }

    public List<SubstitutionMatrixElementDto> getSubstitutionMatrix(FileExportFormat fileFormat) {
        return substitutionMatrixRepo.findByFileFormat(fileFormat).stream().map(this::toSubstitutionMatrixElementDto).collect(toList());
    }


    public List<SubstitutionMatrixElementDetailsDto> getAllSubstitutionMatrix() {
        return substitutionMatrixRepo.getAllSubstitutionMatrix().stream().map(this::toSubstitutionMatrixElementDetailsDto).collect(toList());
    }

    public SingleValue addSubstitution(SubstitutionMatrixElementDto substitutionMatrixDto, FileExportFormat fileExportFormat) {
        SubstitutionMatrixElement substitutionMatrixElement = this.toSubstitutionMatrixElement(substitutionMatrixDto,fileExportFormat);
        substitutionMatrixRepo.save(substitutionMatrixElement);
        auditService.addAudit(substitutionMatrixElement);
        return new SingleValue(substitutionMatrixElement.getId());

    }

    public void deleteSubstitution(Long substitutionId) {
        Optional<SubstitutionMatrixElement> substitutionMatrixElementOptional =substitutionMatrixRepo.findById(substitutionId);
        if (!substitutionMatrixElementOptional.isPresent()) {
            throw new ReglissException(ReglissException.ErrorCode.NOT_FOUND);
        }else {
            SubstitutionMatrixElement substitutionMatrixElement=substitutionMatrixElementOptional.get();
            substitutionMatrixRepo.deleteById(substitutionId);
            auditService.deleteAudit(substitutionMatrixElement);
        }
    }

    public void scheduleCftGtwRequest() { scheduleCftGtwRequest(ImportFileType::isDjFormat); }

    public void scheduleCftGtwRequestAsOfDate(LocalDateTime reqDateTime) {
        scheduleCftGtwRequest(ImportFileType::isWatchlistFormat, reqDateTime);
    }

    public void scheduleSixCftGtwRequestAsOfDate(LocalDateTime reqDateTime, String slot) {
        scheduleSixCftGtwRequest(slot,reqDateTime);
    }
    public void scheduleSixCftGtwRequest(String slot, LocalDateTime... reqDateTime) {
        LocalDate today = LocalDate.now();
        LocalDate todayDate = reqDateTime[0].toLocalDate();
        LocalTime slotTime = LocalTime.parse(slot, SLOT_FMT);
        if (!todayDate.equals(today)) {
            log.info("ScheduleCftGtwRequest: {} : {}", ImportFileType.SIX_LIST_ARCHIVE, reqDateTime);
            cftgtwRequestRepo.save(new CftgtwRequest(ImportFileType.SIX_LIST_ARCHIVE, (reqDateTime.length == 0) ?
                    requestContext.getRequestTime() : reqDateTime[0], requestContext.getCurrentUserRef()));
        }
        LocalTime currentDateTime = LocalTime.now();
        if (slotTime.isAfter(currentDateTime)) {
            log.info("Today’s slot (" + slotTime + ") is in the future - proceed.");
            cftgtwRequestRepo.save(new CftgtwRequest(ImportFileType.SIX_LIST, (reqDateTime.length == 0) ?
                    requestContext.getRequestTime() : reqDateTime[0], requestContext.getCurrentUserRef()));
        } else {
            log.info("Today’s slot (" + slotTime + ") has already passed - reject or reschedule.");
            cftgtwRequestRepo.save(new CftgtwRequest(ImportFileType.SIX_LIST_ARCHIVE,(reqDateTime.length == 0) ?
                    requestContext.getRequestTime() : reqDateTime[0], requestContext.getCurrentUserRef()));
        }
    }


    private void scheduleCftGtwRequest(Predicate<ImportFileType> filterFormat, LocalDateTime... reqDateTime) {
        log.info("ScheduleCftGtwRequest: {} : {}", filterFormat, reqDateTime);
        Stream.of(ImportFileType.values())
                .filter(filterFormat)
                .forEach(djFormat -> cftgtwRequestRepo.save(new CftgtwRequest(djFormat, (reqDateTime.length == 0) ?
                        requestContext.getRequestTime() : reqDateTime[0], requestContext.getCurrentUserRef())));
    }

    public void uploadDjFile(String fileName, InputStream is) throws IOException {
        if(fileName.toUpperCase().startsWith("GSM")){
            sixFileRepo.createInputFileWithContent(fileName, is);
        }else {
            djFileRepo.createInputFileWithContent(fileName, is);
        }
    }

    public PaginatedResults<BatchJobExecutionDto> getAllBatchJobExecutions(PaginationOptions paginationOptions) {
        List<BatchJobExecutionDto> page = batchJobExecutionRepo.findAllWithPagination(paginationOptions).stream()
                .map(this::toBatchJobExecutionDto)
                .collect(Collectors.toList());
        return new PaginatedResults<>(page, batchJobExecutionRepo.count());
    }

    public List<BatchJobExecutionDto> getAllBatchJobExecutionsNoPagination() {
        return batchJobExecutionRepo.getAllBatchJobExecutionsNoPagination(LocalDateTime.now().minusMonths(3)).stream()
                .sorted(Comparator.comparing(BatchJobExecution::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toBatchJobExecutionDto)
                .collect(Collectors.toList());
    }

    public Long countAllBatchJobExecutions(List<BatchJobType> jobType,
                                           List<ExportFile.GenerationStatus> jobStatus,
                                           LocalDateTime dateFrom, LocalDateTime dateTo) {
        boolean jobTypesEmpty = (jobType == null || jobType.isEmpty());
        boolean statusesEmpty = (jobStatus == null || jobStatus.isEmpty());
        boolean includeOk = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.OK);
        boolean includeInProgress = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.IN_PROGRESS);
        boolean includeKo = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.KO);
        return batchJobExecutionRepo.countAllBatchJobExecutions(LocalDateTime.now().minusMonths(MINUS_MONTH_BATCH_JOBS), jobType,
                jobTypesEmpty,
                includeOk, includeInProgress, includeKo,
                dateFrom, dateTo
                , heartbeatCutoffTime, jobCutoffTime
        );
    }


    public List<BatchJobExecutionDto> getAllBatchJobExecutionsWithPagination(Integer currentPage, Integer rowsPerPage,
                                                                             List<BatchJobType> jobType,
                                                                             List<ExportFile.GenerationStatus> jobStatus,
                                                                             LocalDateTime dateFrom, LocalDateTime dateTo) {
        Pageable pagination = PageRequest.of(currentPage-1, rowsPerPage);
        boolean jobTypesEmpty = (jobType == null || jobType.isEmpty());
        boolean statusesEmpty = (jobStatus == null || jobStatus.isEmpty());
        boolean includeOk = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.OK);
        boolean includeInProgress = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.IN_PROGRESS);
        boolean includeKo = statusesEmpty || jobStatus.contains(ExportFile.GenerationStatus.KO);
        return batchJobExecutionRepo.getAllBatchJobExecutionsWithPagination(LocalDateTime.now().minusMonths(MINUS_MONTH_BATCH_JOBS),
                        jobType, jobTypesEmpty,
                        includeOk, includeInProgress, includeKo,
                        dateFrom, dateTo, pagination
                        ,heartbeatCutoffTime, jobCutoffTime
                ).stream()
                .sorted(Comparator.comparing(BatchJobExecution::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toBatchJobExecutionDto)
                .collect(Collectors.toList());
    }

    public void unblockJobExecution(Long batchJobExecutionId) {
        BatchJobExecution batchJobExecution = batchJobExecutionRepo.getExactlyOne(batchJobExecutionId);
        if (batchJobExecution.getJobType().equals(BatchJobType.BATCH_IMPORT_AUTO)) {
            unblockImportService.finishImportExecutionByRequesterUser(batchJobExecution.getId());
            if(batchJobExecution.getVersion() != null) {
                unblockImportService.tryDeleteAutomaticVersionAndRecordsInTx(batchJobExecution.getVersion().getId());
            }
        }
        if (batchJobExecution.getJobType().equals(BatchJobType.BATCH_IMPORT_SEMIAUTO)) {
            unblockImportService.finishImportExecutionByRequesterUser(batchJobExecution.getId());
            if (batchJobExecution.getVersion() != null){
                unblockImportService.tryRevertSemiAutoVersionInTx(batchJobExecution.getVersion().getId());
                importedFileService.getLastUploadedFileForVersion(batchJobExecution.getVersion())
                        .ifPresent(lastUploadedFile -> unblockImportService.unblockSemiAutoImportRegardlessTheStatusInTx(batchJobExecutionId,
                                lastUploadedFile.getId()));
            }
        }
        if(batchJobExecution.getJobType().equals(BatchJobType.BATCH_EXPORT) || batchJobExecution.getJobType().equals(
                BatchJobType.BATCH_EXPORT_SIXRAW)) {
            unblockImportService.finishExportExecutionByRequesterUser(batchJobExecutionId);
        }
        if(batchJobExecution.getJobType().equals(BatchJobType.BATCH_IMPORT_SIXRAW)) {
            unblockImportService.finishImportExecutionByRequesterUser(batchJobExecution.getId());
            unblockImportService.tryDeleteSixAutomaticVersionAndRecordsInTx(batchJobExecutionId);
        }
    }

    public void uploadUserGuide(MultipartFile multipartFile){
        fileTypeVerificationService.checkFile(multipartFile, FileTypeVerificationService.FILE_EXTENSION_PDF);
        User user = requestContext.getCurrentUser();
        userGuideService.persistUserGuideService(multipartFile, user.getUsername());

    }

    public void downloadUserGuideFile(OutputStream outputStream) throws IOException {
        UserGuideFile userGuideFile = userGuideService.getUserGuideFile()
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
        IOUtils.copy(blobService.getBlob(userGuideFile.getFileContent()), outputStream);
    }

    public String getUserGuideFileName(){
        return userGuideService.getUserGuideFile()
                .map(UserGuideFile::getFileName)
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
    }

    public UserGuideDto getUserGuide(){
        return userGuideService.getUserGuideFile()
                .map(userGuide -> userGuideMapper.toDto(userGuide))
                .orElse(null);
    }

    public  SubstitutionMatrixElementDto toSubstitutionMatrixElementDto(SubstitutionMatrixElement entity) {
        SubstitutionMatrixElementDto dto = new SubstitutionMatrixElementDto();
        dto.id = entity.getId();
        dto.originalValue = entity.getOriginalValue();
        dto.substitutionValue = entity.getSubstitutionValue();

        return dto;
    }

    public SubstitutionMatrixElementDetailsDto toSubstitutionMatrixElementDetailsDto(SubstitutionMatrixElement entity) {
        SubstitutionMatrixElementDetailsDto dto = new SubstitutionMatrixElementDetailsDto();
        dto.id = entity.getId();
        dto.fileFormat = entity.getFileFormat();
        dto.originalValue = entity.getOriginalValue();
        dto.substitutionValue = entity.getSubstitutionValue();

        return dto;
    }


    private SubstitutionMatrixElement toSubstitutionMatrixElement(SubstitutionMatrixElementDto dto, FileExportFormat fileFormat) {
        SubstitutionMatrixElement entity = new SubstitutionMatrixElement();
        entity.setId(dto.id);
        entity.setOriginalValue(dto.originalValue.replace("\\n", "\n").replace("\\r", "\r"));
        entity.setSubstitutionValue(dto.substitutionValue);
        entity.setFileFormat(fileFormat);

        return entity;
    }

    private BatchJobExecutionDto toBatchJobExecutionDto(BatchJobExecution entity) {
        BatchJobExecutionDto dto = new BatchJobExecutionDto();
        dto.id = entity.getId();
        dto.nodeId = entity.getNodeId();
        dto.hostName = entity.getHostName();
        dto.jobType = entity.getJobType();
        dto.jobParams = mapJobParams(entity);
        dto.percent = entity.getPercent();
        dto.startDate = entity.getStartDate();
        dto.endDate = entity.getEndDate();
        dto.isJobAlive = batchJobExecutionService.isJobAlive(entity);
        dto.status = ExportFile.GenerationStatus.valueOf(computeStatus(entity));
        return dto;
    }

    private String computeStatus(BatchJobExecution entity) {
        if(entity.getEndDate()!=null) {
            return "OK";
        }
        return batchJobExecutionService.isJobAlive(entity) ? "IN_PROGRESS" : "KO";
    }


    private String mapJobParams(BatchJobExecution entity) {
        switch (entity.getJobType()) {
            case BATCH_EXPORT:
                return formatParameters(entity.getJobParams(), BatchExportLoggedParameters.class);
            case BATCH_IMPORT_SEMIAUTO:
                return formatParameters(entity.getJobParams(), BatchImportSemiAutoLoggedParameters.class);
            case BATCH_IMPORT_AUTO:
                return formatParameters(entity.getJobParams(), BatchImportAutoLoggedParameters.class);
            case BATCH_IMPORT_SIXRAW:
                return formatParameters(entity.getJobParams(), BatchImportAutoLoggedParameters.class);
            case BATCH_EXPORT_SIXRAW:
                return formatParameters(entity.getJobParams(), BatchExportLoggedParameters.class);
            default:
                throw new IllegalArgumentException("Not a valid job type: " + entity.getJobType());
        }
    }

    private <T> String formatParameters(String json, Class<T> valueType) {
        return JacksonUtil.getDeserializedValue(json, valueType)
                .map(value -> ((BatchJobParameterLoggable) value).getPrettyValue())
                .orElse(json);
    }

    public LocalDate getLatestWLImportOKDate() { return  importedFileService.getLatestWLImportOKDate(); }
}

