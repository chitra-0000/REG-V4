package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.exporter.GeneratedFileService;
import com.bnpp.regliss.facade.dto.VersionBreadcrumbDto;
import com.bnpp.regliss.facade.dto.record.ProgramDto;
import com.bnpp.regliss.facade.dto.record.RecordVerificationInfoDto;
import com.bnpp.regliss.facade.dto.version.*;
import com.bnpp.regliss.facade.mapper.DJCoreSynthesisProgramsFilter;
import com.bnpp.regliss.facade.mapper.VersionMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.service.*;
import com.bnpp.regliss.service.VersionService.FileIdsToGenerate;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.service.verification.RecordVerificationService;
import com.bnpp.regliss.utils.JacksonUtil;
import com.bnpp.regliss.vo.AddModSupAction;
import com.bnpp.regliss.vo.AddModSupLineForSynthesis;
import com.bnpp.regliss.vo.ImportLineErrorReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Facade
@ReglissWebProfile
@Slf4j
public class VersionFacade {

    @Value("${version.delete.timeout.seconds:500}")
    private int versionDeletetimeoutSeconds;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private ReglementationService reglementationService;

    @Autowired
    private AddModSupRepository addModSupRepo;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private VersionAuditService versionAuditService;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private RecordVerificationService recordVerificationService;

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private VersionLockService lockService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private ReglissListRepository reglissListRepo;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private BatchExportService batchExportService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private GeneratedFileService generatedFileService;

    public Long createManualVersion(Long listId) {
        ReglissList list = reglissListRepo.getExactlyOne(listId);
        list.getCurrentVersion().ifPresent(version -> {
            if(!version.wasCertified())
                throw new ReglissException(ErrorCode.CREATE_VERSION_ALREADY_EXISTS);
        });

        return versionService.createVersion(listId).getId();
    }


    public VersionDtoView getVersion(long versionId) {
        Version version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        if(version.getStatus() == VersionStatus.DRAFT && version.wasAutoUploaded()){
            throw new ReglissException(ErrorCode.AUTOMATIC_VERSION_IN_PROGRESS);
        }

        if (!versionPermissions.isVersionReadOnly(version) || versionPermissions.getActions(version).delete) {
            lockService.lock(version);
        }
        Optional<Version> previousVersion =
                versionRepo.getPreviousVersionById(version.getList().getId(), version.getId());
        versionAuditService.auditGet(version);
        VersionDtoView versionDto = versionMapper.toVersionDtoView(version,previousVersion);
        versionDto.previousVersionId = previousVersion.map(Version::getId).orElse(null);
        versionDto.previousVersionNumber = previousVersion.map(Version::getVersionNumber).orElse(null);
        versionDto.isReadOnly = versionPermissions.isVersionReadOnly(version);
        versionDto.actions = versionPermissions.getActions(version);
        versionDto.isImportedFileUploaded = version.getImportedFileOk().isPresent();
        return versionDto;
    }

    public VersionSynthesisDto getVersionForSynthesis(long versionId) {
        Version version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        if(version.getStatus() == VersionStatus.DRAFT && version.wasAutoUploaded()){
            throw new ReglissException(ErrorCode.AUTOMATIC_VERSION_IN_PROGRESS);
        }

        if (!versionPermissions.isVersionReadOnly(version) || versionPermissions.getActions(version).delete) {
            lockService.lock(version);
        }

        VersionSynthesisDto dto = versionMapper.toVersionSynthesisDto(version);
        dto.actions = versionMapper.extractActions(versionPermissions.getActions(version));
        return dto;
    }

    public VersionBreadcrumbDto getBreadcrumbForVersion(long versionId){
        ListVersionBreadcrumbDto version = versionRepo.getBreadcrumbForVersion(versionId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        return  versionMapper.toVersionBreadcrumDto(version);
    }

    public void updateVersion(VersionDtoForm dto) {
        Version versionInDb = versionRepo.getExactlyOne(dto.id);
        lockService.checkAndExtendLock(versionInDb);
        Version oldVersion = new Version(versionInDb);
        Version newVersion = versionMapper.toEntity(versionInDb, dto);
        if(!dto.isTemporary){
            recordVerificationService.updateVerificationOnSaveOrVerify(versionInDb);
            generatedFileService.updateAddModSupAuditTrailFileAsDeprecated(versionInDb);
        }
        versionAuditService.auditUpdate(oldVersion, newVersion);
    }

    public void updateVersionInSynthesis(long versionId, RecordVerificationInfoDto dto) {
        Version versionInDb = versionRepo.getExactlyOne(versionId);
        lockService.checkAndExtendLock(versionInDb);
        recordVerificationService.updateVersionStatusAndComment(dto, versionInDb);
        generatedFileService.updateAddModSupAuditTrailFileAsDeprecated(versionInDb);
    }

    public void saveOrUpdateTemporaryChange(Long versionId, RecordVerificationInfoDto verifiedRecord) {
        Version versionInDb = versionRepo.getExactlyOne(versionId);
        lockService.checkAndExtendLock(versionInDb);
        recordVerificationService.insertOrUpdateRecordVerificationChanges(verifiedRecord, versionInDb);
    }

    public void validateVersion(long id) {
        Version version = versionRepo.getExactlyOne(id);
        lockService.checkAndExtendLock(version);
        VersionStatus previousStatus = version.getStatus();
        version.validate(requestContext.getCurrentUserRef());
        emailService.sendEmailValidationOk(version);
        versionAuditService.auditValidate(previousStatus, version);
    }

    public void returnVersion(long id, String returnComment) {
        Version version = versionRepo.getExactlyOne(id);
        lockService.checkAndExtendLock(version);
        String oldReturnComment = version.getReturnComment();
        version.returnForValidation(requestContext.getCurrentUserRef(), returnComment);
        versionAuditService.auditReturn(version, oldReturnComment);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void certifyOrConfirmVersion(long versionId, String confirmationMessage) {
        FileIdsToGenerate filesToGenerate = versionService.certifyVersionInTx(versionId, confirmationMessage);
        log.info("Updated version {} status on confirm or certify by user",versionId);
        batchExportService.persistVersionExportGenerationAtCertificationOrConfirmation(versionId, filesToGenerate);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteVersion(long id) {
