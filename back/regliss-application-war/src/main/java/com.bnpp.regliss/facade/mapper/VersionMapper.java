package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.ImportedFile.UploadStatus;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;
import com.bnpp.regliss.facade.dto.VersionBreadcrumbDto;
import com.bnpp.regliss.facade.dto.list.ListVersionDto;
import com.bnpp.regliss.facade.dto.list.VersionForVersionPageDto;
import com.bnpp.regliss.facade.dto.record.ProgramDto;
import com.bnpp.regliss.facade.dto.record.RecordVerificationInfoDto;
import com.bnpp.regliss.facade.dto.version.*;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.service.permission.VersionActions;
import com.bnpp.regliss.service.permission.VersionPermissionRequest;
import com.bnpp.regliss.service.permission.VersionSynthesisActions;
import com.bnpp.regliss.vo.AddModSupLine;
import com.bnpp.regliss.vo.AddModSupLineForSynthesis;
import com.bnpp.regliss.vo.ListVersionVO;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.ZoneId;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@ReglissWebProfile
public class VersionMapper {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    @Autowired
    private VersionLockService versionLockService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private CurrentVersionPermissions currentVersionPermissions;

    public String getVersionStatusTranslationKey(VersionStatus status, boolean wasAutoUploaded) {
        if (wasAutoUploaded) {
            if (status == VersionStatus.VALIDATED)
                return "INTEGRATED";
            if (status == VersionStatus.CERTIFIED)
                return "CONFIRMED";
        }
        return status.name();
    }

    public String translateVersionStatus(VersionStatus status, boolean wasAutoUploaded) {
        String translationKey = "Version.Status." +getVersionStatusTranslationKey(status, wasAutoUploaded);
        return messageSource.getMessage(translationKey, null, requestContext.getLocale());
    }

    public ListVersionDto toListVersionDto(ListVersionVO version, ReglissList list) {
        ListVersionDto dto = new ListVersionDto();
        dto.id = version.getId();
        dto.versionNumber = version.getVersionNumber();
        dto.status = translateVersionStatus(version.getStatus(), wasAutoUploaded(version.getUploadType(), list));
        dto.statusValue = version.getStatus();
        dto.publicationDate = version.getPublicationDate();
        dto.startDate = version.getStartDate() != null ? version.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
        dto.username = version.getUsername();
        VersionPermissionRequest permision =
                new VersionPermissionRequest(version.getCertificationDate(), version.getPublicationDate(), version.getStatus(), list);
        dto.isReadOnly = currentVersionPermissions.isVersionReadOnlyForDisplayInList(permision);
        return dto;
    }

    private boolean wasAutoUploaded(ImportedFile.UploadType uploadType, ReglissList list) {
        if(list.allowsAutoUpload() && !list.allowsManualUpload()){
            return true;
        }
        return uploadType == ImportedFile.UploadType.AUTO;
    }

    public ListVersionDto toListVersionDto(Version version) {
        ListVersionDto dto = new ListVersionDto();
        dto.id = version.getId();
        dto.versionNumber = version.getVersionNumber();
        dto.status = translateVersionStatus(version.getStatus(), version.wasAutoUploaded());
        dto.statusValue = version.getStatus();
        dto.publicationDate = version.getPublicationDate();
        dto.startDate = version.getStartDate();
        dto.username = version.getCreationUser().getUsername();
        dto.isReadOnly = currentVersionPermissions.isVersionReadOnlyForDisplayInList(new VersionPermissionRequest(version));
        return dto;
    }

    public void fillVersionsLockDetails(List<ListVersionDto> dtos) {
        Set<Long> versionIds = dtos.stream().map(ListVersionDto::getId).collect(toSet());
        if (versionIds.isEmpty()) {
            return;
        }
        Map<Long, VersionLock> allLocks = versionLockService.getLastActiveLocks(versionIds);
        for (ListVersionDto versionDto : dtos) {
            VersionLock versionLock = allLocks.get(versionDto.id);
            if (versionLock != null && !versionLock.isOwnedBy(requestContext.getUserId())) {
                versionDto.isLocked = true;
                versionDto.lockingUser = versionLock.getUser().getUsername();
            }
        }
    }

    public Version toEntity(Version entity, VersionDtoForm dto) {
        entity.setLegalBase(dto.legalBase);
        entity.setStartDate(dto.startDate);
        entity.setCommentFr(dto.commentFr);
        entity.setCommentEn(dto.commentEn);
        return entity;
    }

    public VersionSynthesisDto toVersionSynthesisDto(Version version) {
        VersionSynthesisDto dto = new VersionSynthesisDto();
        dto.id = version.getId();
        Optional<AddModSupExportFile> lastAddModSupExportFileOpt = version.getLastAddModSupExportFileOpt();
        dto.canRegenerateAddModSupFile = lastAddModSupExportFileOpt
                .map(AddModSupExportFile::isNotCorrectlyGenerated)
                .orElse(true);
        Optional<AddModSupAuditTrailFile> lastAddModSupAuditTrailFileOpt = version.getLastAddModSupAuditTrailFileOpt();
        Optional<AddModSupAuditTrailFile> lastOKAddModSupAuditTrailFileOpt = version.getLastOKAddModSupAuditTrailFileOpt();
        dto.canDownloadAddModSupAuditTrailFile = canDownloadAddModSupAuditTrailFile(lastOKAddModSupAuditTrailFileOpt,
                lastAddModSupAuditTrailFileOpt);
        dto.canRegenerateAddModSupAuditTrailFile = lastAddModSupAuditTrailFileOpt
                .map(this::isDeprecatedOrKO)
                .orElse(true);
        dto.isDeprecatedAddModSupAuditTrailFile = isDeprecatedAddModSupAuditTrailFile(lastOKAddModSupAuditTrailFileOpt,
                lastAddModSupAuditTrailFileOpt);

        return dto;
    }

    public VersionSynthesisActions extractActions(VersionActions actions) {
        VersionSynthesisActions partialActions = new VersionSynthesisActions();
        partialActions.addModSupAuditTrailDownload = actions.addModSupAuditTrailDownload;
        partialActions.addModSupAuditTrailRegenerate = actions.addModSupAuditTrailRegenerate;
        partialActions.addModSupDownload = actions.addModSupDownload;
        partialActions.addModSupRegenerate = actions.addModSupRegenerate;
        return partialActions;
    }

    public VersionDtoView toVersionDtoView(Version version, Optional<Version> previousVersion) {
        VersionDtoView dto = new VersionDtoView();
        dto.id = version.getId();
        dto.versionNumber = version.getVersionNumber();
        dto.status = version.getStatus();
        dto.legalBase = version.getLegalBase();
        dto.startDate = version.getStartDate();
        dto.commentFr = version.getCommentFr();
        dto.commentEn = version.getCommentEn();
        dto.importInProgress = version.getImportedFiles().stream()
                .anyMatch(file -> file.getStatus() == UploadStatus.IMPORTING || file.getStatus() == UploadStatus.UPLOAD_OK);
        dto.wasAutoUploaded = version.wasAutoUploaded();

        dto.fileName = version.getFileName();

        dto.validationDate = version.getValidationDate();
        dto.validationUser = version.getValidationUserOpt().map(User::getUsername).orElse("");
        dto.certificationDate = version.getCertificationDate();
        dto.certificationUser = version.getCertificationUserOpt().map(User::getUsername).orElse("");
        dto.publicationDate = version.getPublicationDate();
        dto.verificationDate = version.getVerificationDate();
        dto.listName = version.getList().getShortName();
        dto.breadcrumbDto = new BreadcrumbDto();
        dto.breadcrumbDto.idList = version.getList().getId();
        dto.breadcrumbDto.listName = version.getList().getShortName();
        dto.breadcrumbDto.listReglissReference = version.getList().getReference();
        dto.breadcrumbDto.versionNumber = version.getVersionNumber();
        dto.breadcrumbDto.importFileType =  version.getImportFileType();

        dto.isLocked = versionLockService.isLockedByAnotherUser(version.getId());
        if (dto.isLocked) {
            dto.lockingUser = versionLockService.getLockingUser(version.getId()).map(User::getUsername).orElse(null);
        }
        dto.isVerified = version.isVerified();
        dto.listConfidentiality = version.getList().getConfidentiality();

        dto.computedRecordNumbersDto = toComputedRecordNumbersDto(version.getComputedRecordNumbers());
        Optional<AddModSupExportFile> lastAddModSupExportFileOpt = version.getLastAddModSupExportFileOpt();
        dto.canRegenerateAddModSupFile = lastAddModSupExportFileOpt
                .map(AddModSupExportFile::isNotCorrectlyGenerated)
                .orElse(true);
        dto.addModSupGenerationStatus = lastAddModSupExportFileOpt
                .map(AddModSupExportFile::getGenerationStatus)
                .orElse(null);

        Optional<AddModSupAuditTrailFile> lastAddModSupAuditTrailFileOpt = version.getLastAddModSupAuditTrailFileOpt();
        Optional<AddModSupAuditTrailFile> lastOKAddModSupAuditTrailFileOpt = version.getLastOKAddModSupAuditTrailFileOpt();
        dto.canDownloadAddModSupAuditTrailFile = canDownloadAddModSupAuditTrailFile(lastOKAddModSupAuditTrailFileOpt,
                lastAddModSupAuditTrailFileOpt);
        dto.canRegenerateAddModSupAuditTrailFile = lastAddModSupAuditTrailFileOpt
                .map(this::isDeprecatedOrKO)
                .orElse(true);
        dto.isDeprecatedAddModSupAuditTrailFile = isDeprecatedAddModSupAuditTrailFile(lastOKAddModSupAuditTrailFileOpt,
                lastAddModSupAuditTrailFileOpt);

        dto.allowsAutoUpload = version.getList().allowsAutoUpload();
        dto.allowsManualUpload = version.getList().allowsManualUpload();
        dto.importFileType = version.getImportFileType();
        dto.numberOfRecordsInPreviousVersion = previousVersion.map(v ->
                Optional.ofNullable(v.getComputedRecordNumbers()).map(ComputedRecordNumbers::getTotal).orElse(0L)
        ).orElse(0L);

        dto.showImportErrors =   importedFileRepository.findTop1ByVersionOrderByUploadDateDesc(version).map(ImportedFile::isImportKO)
                .orElse(false) && version.getStatus() == VersionStatus.DRAFT;
        return dto;
    }

    public VersionBreadcrumbDto toVersionBreadcrumbDto(ListVersionBreadcrumbDto version) {
        VersionBreadcrumbDto dto = new VersionBreadcrumbDto(version);
        return dto;
    }

    public ComputedRecordNumbersDto toComputedRecordNumbersDto(ComputedRecordNumbers computedRecordNumbers) {
        ComputedRecordNumbersDto dto = new ComputedRecordNumbersDto();
        dto.add = computedRecordNumbers.getAdd();
        dto.mod = computedRecordNumbers.getMod();
        dto.strong = computedRecordNumbers.getStrong();
        dto.sup = computedRecordNumbers.getSup();
        dto.total = computedRecordNumbers.getTotal();
        dto.weak = computedRecordNumbers.getWeak();
        dto.modForSynthesis  = computedRecordNumbers.getModForSynthesis();
        return dto;
    }

    public List<ProgramDto> toProgramDtos(List<Program> programs) {
        return programs.stream()
                .map(p -> new ProgramDto(p.getId(), p.getLabelOpt().orElse("")))
                .sorted(Comparator.comparing(ProgramDto::getLabel))
                .distinct()
                .collect(toList());
    }

    public VersionForVersionPageDto toVersionForVersionPageDto(ListVersionVO version, Long minorUpdates,ReglissList list) {
        VersionForVersionPageDto versionDto = new VersionForVersionPageDto();
        versionDto.id = version.getId();
        versionDto.publicationDate = version.getPublicationDate();
        versionDto.startDate = (version.getStartDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        versionDto.versionNumber = version.getVersionNumber();
        versionDto.status=version.getStatus();
        versionDto.created = version.getCreated();
        versionDto.updated = version.getUpdated();
        versionDto.deleted = version.getDeleted();
        versionDto.minorUpdates = minorUpdates;
        versionDto.recordNumber = version.getRecordNumber();
        versionDto.isAutoImport = wasAutoUploaded(version.getUploadType(), list);
        versionDto.hasMinorUpdates = !list.getImportFileType().isWatchlistFormat();
        return versionDto;
    }

    public VersionSynthesisRowDto toVersionSynthesisRowDto(AddModSupLineForSynthesis lineForSynthesis) {
        VersionSynthesisRowDto dto = new VersionSynthesisRowDto();
        AddModSupLine addModSupLine = lineForSynthesis.getAddModSupLine();
        dto.fullName = addModSupLine.getPrimaryName();
        dto.id = addModSupLine.getId();
        dto.recordReglissReference = addModSupLine.getAkaReglissReferenceRaw().orElse(addModSupLine.getRecordReglissReference());
        dto.recordId = addModSupLine.getId();
        dto.recordType = referenceService.translateEnum(addModSupLine.getRecordType());

        dto.modElement = addModSupLine.getModElement();
        setOldAndNewValues(dto, addModSupLine);

        dto.childId = addModSupLine.getChildId();
        dto.childType = addModSupLine.getChildType();
        dto.auditedAction = addModSupLine.getAuditedAction();
        dto.externalUuid = lineForSynthesis.getExternalUuid();
        dto.verificationComment = isTemporaryVerificationComment(lineForSynthesis) ? lineForSynthesis.getTemporaryVerificationComment()
                : lineForSynthesis.getVerificationComment();
        dto.finalVerificationComment = lineForSynthesis.getVerificationComment();
        dto.isTemporaryVerificationComment = isTemporaryVerificationComment(lineForSynthesis);
        dto.verificationStatus = isTemporaryVerificationStatus(lineForSynthesis) ? lineForSynthesis.getTemporaryVerificationStatus()
                : lineForSynthesis.getVerificationStatus();
        dto.finalVerificationStatus = lineForSynthesis.getVerificationStatus();
        dto.isTemporaryVerificationStatus = isTemporaryVerificationStatus(lineForSynthesis);

        return dto;
    }

    private boolean isTemporaryVerificationComment(AddModSupLineForSynthesis lineForSynthesis){
        if( null == lineForSynthesis.getTemporaryRecordId() )
            return false;
        return isTemporaryComment(lineForSynthesis);
    }

    private boolean isTemporaryComment(AddModSupLineForSynthesis lineForSynthesis){
        return (null == lineForSynthesis.getVerificationComment() ? null != lineForSynthesis.getTemporaryVerificationComment() :
                !lineForSynthesis.getVerificationComment().equals(lineForSynthesis.getTemporaryVerificationComment()));
    }

    private boolean isTemporaryVerificationStatus(AddModSupLineForSynthesis lineForSynthesis){
        if(null == lineForSynthesis.getTemporaryRecordId() )
            return false;
        return lineForSynthesis.getVerificationStatus() != lineForSynthesis.getTemporaryVerificationStatus();
    }

    private void setOldAndNewValues(VersionSynthesisRowDto dto, AddModSupLine addModSupLine) {
        switch (addModSupLine.getAuditedAction()) {
            case RECORD_ADDRESS_MODIFICATION:
            case RECORD_ADDRESS_ADDITION:
            case RECORD_ADDRESS_DELETION:
            case RECORD_COUNTRY_DETAILS_MODIFICATION:
            case RECORD_COUNTRY_DETAILS_ADDITION:
            case RECORD_COUNTRY_DETAILS_DELETION:
                dto.newValue = getCountryIfIdOtherwiseReturnString(addModSupLine.getNewValue());
                dto.oldValue = getCountryIfIdOtherwiseReturnString(addModSupLine.getOldValue());
                break;
            default:
                dto.newValue = addModSupLine.getNewValue();
                dto.oldValue = addModSupLine.getOldValue();
                break;
        }
    }

    private String getCountryIfIdOtherwiseReturnString(String country) {
        return NumberUtils.isParsable(country) ?
                referenceService.getCountryById(Long.parseLong(country)).getLabel() :
                country;
    }

    private boolean isDeprecatedOrKO(AddModSupAuditTrailFile addModSupAuditTrailFile){
        return addModSupAuditTrailFile.isDeprecated() || addModSupAuditTrailFile.isKO();
    }

    private boolean isDeprecatedAddModSupAuditTrailFile(Optional<AddModSupAuditTrailFile> lastOKAddModSupAuditTrailFileOpt,
                                                        Optional<AddModSupAuditTrailFile> lastAddModSupAuditTrailFileOpt) {
        if (lastOKAddModSupAuditTrailFileOpt.isPresent() && lastAddModSupAuditTrailFileOpt.isPresent()) {
            AddModSupAuditTrailFile lastOKAddModSupAuditTrailFile = lastOKAddModSupAuditTrailFileOpt.get();
            AddModSupAuditTrailFile lastAddModSupAuditTrailFile = lastAddModSupAuditTrailFileOpt.get();
            if (lastOKAddModSupAuditTrailFile.getId().equals(lastAddModSupAuditTrailFile.getId())) {
                return lastOKAddModSupAuditTrailFile.isDeprecated();
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean canDownloadAddModSupAuditTrailFile(Optional<AddModSupAuditTrailFile> lastOKAddModSupAuditTrailFileOpt,
                                                       Optional<AddModSupAuditTrailFile> lastAddModSupAuditTrailFileOpt){
        if( !lastAddModSupAuditTrailFileOpt.isPresent())
            return false;
        else
            return !lastAddModSupAuditTrailFileOpt.get().isInProgress() && lastOKAddModSupAuditTrailFileOpt.isPresent();
    }

    public FileToRegenerateDto toFilterToRegenerateDto(DJExportFilter filter){
        FileToRegenerateDto dto=new FileToRegenerateDto();
        dto.formatOrFilterName=filter.getFilterName();
        dto.type=filter.getType();
        return dto;
    }
}

