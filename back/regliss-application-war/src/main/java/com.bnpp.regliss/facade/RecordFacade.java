package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ListPermission;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.exporter.writer.format.regular.ExportFormatters;
import com.bnpp.regliss.facade.dto.RecordCountSearchDto;
import com.bnpp.regliss.facade.dto.record.*;
import com.bnpp.regliss.facade.dto.version.RecordInVersionSearchResultDto;
import com.bnpp.regliss.facade.mapper.*;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.repository.pagination.PaginatedResults;
import com.bnpp.regliss.search.RecordCompareVersionsCriteria;
import com.bnpp.regliss.search.RecordSearchCriteria;
import com.bnpp.regliss.search.RecordSearchVersionCriteria;
import com.bnpp.regliss.service.RecordAuditService;
import com.bnpp.regliss.service.RecordService;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.service.permission.*;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.bnpp.regliss.vo.CompareVersionsLine;
import com.bnpp.regliss.vo.ListVersionVO;
import com.bnpp.regliss.vo.RecordAndVersionForSearchVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bnpp.regliss.utils.TextNormalizerQueryUtil.normalizePartialMatchCriteria;

@Facade
@Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class RecordFacade {

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private RecordMapper mapper;

    @Autowired
    private RecordSearchMapper searchMapper;

    @Autowired
    private RecordDtoMapper dtoMapper;

    @Autowired
    private RecordAuditService recordAuditService;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private RecordSearchRepository recordSearchRepo;

    @Autowired
    private RecordService recordService;

    @Autowired
    private CurrentListPermissions listPermissions;

    @Autowired
    private CurrentVersionPermissions versionPermissions;

    @Autowired
    private VersionLockService lockService;

    @Autowired
    private RecordAssociationReadRepository associationRepo;

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private RecordDescriptionMapper recordDescriptionMapper;

    @Value("${record.search.count.limit}")
    private Long recordSearchCountLimit;
    @Autowired
    private ExportToPdfMapperService mapperService;

    public PaginatedResults<RecordInVersionSearchResultDto> searchRecordsForVersion(RecordSearchVersionCriteria criteria) {
        recordAuditService.logSearch(criteria);
        Version version = versionRepo.getExactlyOne(criteria.idVersion);
        criteria.name = normalizePartialMatchCriteria(criteria.name);
        criteria.program = normalizePartialMatchCriteria(criteria.program);
        criteria.codeStr = normalizePartialMatchCriteria(criteria.codeStr);
        PaginatedResults<Record> paginatedEntities = recordRepo.searchForVersion(criteria, version.getList().getId());
        return paginatedEntities.map(record -> searchMapper.toRecordSearchForVersionDto(record, version));
    }

    private void normalizePartialMatchCriteriaForRecordSearch(RecordSearchCriteria criteria) {
        criteria.name = normalizePartialMatchCriteria(criteria.name);
        criteria.program = normalizePartialMatchCriteria(criteria.program);
        criteria.codeStr = normalizePartialMatchCriteria(criteria.codeStr);
    }

    @Transactional(propagation = Propagation.REQUIRED, timeout = 599)
    public PaginatedResults<RecordSearchResultDto> searchRecordsForPage(RecordSearchCriteria criteria) {
        recordAuditService.logSearch(criteria);
        Set<Long> allowedListsIds = listPermissions.getAllowedListsIdsForCurrentUser();
        normalizePartialMatchCriteriaForRecordSearch(criteria);
        List<RecordAndVersionForSearchVO> recordPage = recordSearchRepo.queryForPage(criteria, allowedListsIds);
        PaginatedResults<RecordAndVersionForSearchVO> paginatedEntities = new PaginatedResults<>(recordPage, -1);
        return paginatedEntities.map(record -> searchMapper.toRecordSearchDto(record));
    }

    @Transactional(propagation = Propagation.REQUIRED, timeout = 599)
    public RecordCountSearchDto searchRecordsForTotal(RecordSearchCriteria criteria) {
        return new RecordCountSearchDto(getTotalRecordsForSearch(criteria), recordSearchCountLimit);
    }
    public long getTotalRecordsForSearch(RecordSearchCriteria criteria) {
        Set<Long> allowedListsIds = listPermissions.getAllowedListsIdsForCurrentUser();
        normalizePartialMatchCriteriaForRecordSearch(criteria);
        return PerformanceUtil.runAndMeasure("RecordSearchForTotal", () -> recordSearchRepo.queryForTotal(criteria, allowedListsIds));
    }


    public RecordDto getRecord(String uuid, Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        Record record = recordRepo.findByExternalUuidAndListId (uuid, version.getList().getId())
            .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        versionPermissions.checkVersionAccessForViewRecords(new VersionPermissionRequest(version));

        lockVersionIfDraftReturnedOrReleaseLockIfNotDraft(version);
        recordAuditService.auditGet(record, version);
        RecordDto recordDto = mapper.toDto(record, version);
        if(!record.getSanctionReferences().isEmpty() &&
                (version.getImportFileType().name().equals(ImportFileType.DOW_JONES_WATCHLIST.name())
                        ||
                        version.getImportFileType().name().equals(ImportFileType.DOW_JONES_CORE.name())
                        ||
                        version.getImportFileType().name().equals(ImportFileType.DOW_JONES_AME.name()))) {
            Set<LocalDate> sanctionSinceDate=record.getSanctionReferences().stream().filter(sr ->
                    ExportFormatters.getFormatedDateForExport(sr.getSinceDateStr())!=null).map(sr ->
                    ExportFormatters.getFormatedDateForExport(sr.getSinceDateStr())).collect(Collectors.toSet());
            if(!StringUtils.isEmpty(record.getStartDateStr()) && ExportFormatters.getFormatedDateForExport(record.getStartDateStr())!=null) {
                sanctionSinceDate.add(ExportFormatters.getFormatedDateForExport(record.getStartDateStr()));
            }
            Optional<LocalDate> date = sanctionSinceDate.stream().min(Comparator.comparing(LocalDate::toEpochDay));
            String sanctionDate = "";
            if (date.isPresent()) {
                sanctionDate = date.get().toString();
            }
            recordDto.generalFields.startDate = sanctionDate;
        }
        recordDto.generalFields.readOnly = !versionPermissions.getActions(version).editRecords;
        ListPermission listPermission = listPermissions.getCurrentPermissionForList(new ListPermissionRequest(version.getList()));
        recordDto.hasWritePermissionOnList = listPermission == ListPermission.WRITE;
        recordDto.jasperPdfDownload = recordService.jasperPdfDownload(version);
        recordDto.generalFields.relations.addAll(
                mapperService.ifWatchlistAceptanceList(record.getExternalUuid(), version.getId()));
        return recordDto;
    }

    public RecordDto getRecordView(String reglissReference, Long idVersion) {
        Version version = versionRepo.getExactlyOne(idVersion);
        Record record = recordRepo.findByReglissReferenceAndVersionIdAndListId (reglissReference, version.getId(), version.getList().getId())
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        versionPermissions.checkVersionAccessForViewRecords(new VersionPermissionRequest(version));

        lockVersionIfDraftReturnedOrReleaseLockIfNotDraft(version);
        recordAuditService.auditGet(record, version);
        RecordDto recordDto = mapper.toDto(record, version);
        recordDto.generalFields.readOnly = true;
        ListPermission listPermission = listPermissions.getCurrentPermissionForList(new ListPermissionRequest(version.getList()));
        recordDto.hasWritePermissionOnList = listPermission == ListPermission.WRITE;
        recordDto.jasperPdfDownload = recordService.jasperPdfDownload(version);
        return recordDto;
    }
    public RecordDto getRecordByReferenceAndVersionNo(String reglissReference, Long versionNo, Long listId) {
        Version version = versionRepo.getByVersionNumberAndListId(versionNo, listId)
                .orElseThrow(()-> new ReglissException(ErrorCode.NOT_FOUND));
        Record record = recordRepo.findByReglissReferenceAndVersionIdAndListId (reglissReference, version.getId(), listId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        versionPermissions.checkVersionAccessForViewRecords(new VersionPermissionRequest(version));

        lockVersionIfDraftReturnedOrReleaseLockIfNotDraft(version);
        recordAuditService.auditGet(record, version);
        RecordDto recordDto = mapper.toDto(record, version);
        if(!record.getSanctionReferences().isEmpty() &&
                (version.getImportFileType().name().equals(ImportFileType.DOW_JONES_WATCHLIST.name())
                        ||
                        version.getImportFileType().name().equals(ImportFileType.DOW_JONES_CORE.name())
                        ||
                        version.getImportFileType().name().equals(ImportFileType.DOW_JONES_AME.name()))) {
            Set<LocalDate> sanctionSinceDate=record.getSanctionReferences().stream().filter(sr ->
                    ExportFormatters.getFormatedDateForExport(sr.getSinceDateStr())!=null).map(sr ->
                    ExportFormatters.getFormatedDateForExport(sr.getSinceDateStr())).collect(Collectors.toSet());
            if(!StringUtils.isEmpty(record.getStartDateStr()) && ExportFormatters.getFormatedDateForExport(record.getStartDateStr())!=null) {
                sanctionSinceDate.add(ExportFormatters.getFormatedDateForExport(record.getStartDateStr()));
            }
            Optional<LocalDate> date = sanctionSinceDate.stream().min(Comparator.comparing(LocalDate::toEpochDay));
            String sanctionDate = "";
            if (date.isPresent()) {
                sanctionDate = date.get().toString();
            }
            recordDto.generalFields.startDate = sanctionDate;
        }

        recordDto.generalFields.readOnly = true;
        ListPermission listPermission = listPermissions.getCurrentPermissionForList(new ListPermissionRequest(version.getList()));
        recordDto.hasWritePermissionOnList = listPermission == ListPermission.WRITE;
        recordDto.jasperPdfDownload = recordService.jasperPdfDownload(version);
        return recordDto;
    }
    public String updateRecord(RecordDto dto) {
        Version version = versionRepo.getExactlyOne(dto.generalFields.idVersion);
        lockService.checkAndExtendLock(version);
        Record newRecord = dtoMapper.toEntity(dto);
        return recordService.update(newRecord, version);
    }

    public void createRecord(RecordDto dto) {
        Version version = versionRepo.getExactlyOne(dto.generalFields.idVersion);
        lockService.checkAndExtendLock(version);
        Record newRecord = dtoMapper.toEntity(dto);
        recordService.createRecord(newRecord, version);
    }

    public void deleteRecord(String uuid, Long versionId) {
        Version version = versionRepo.getExactlyOne(versionId);
        lockService.checkAndExtendLock(version);
        Record record = recordRepo.findByUUIDAndVersionId(uuid, versionId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        recordService.deleteRecord(record, version);
    }

    private void lockVersionIfDraftReturnedOrReleaseLockIfNotDraft(Version version) {
        if (version.statusIsOneOf(VersionStatus.DRAFT, VersionStatus.RETURNED)) {
            lockService.lock(version);
        } else {
            lockService.unlock(version);
        }
    }

    public List<RecordAssociationDto> getRecordAssociations(String uuid) {
        return associationRepo.getRecordAssociations(uuid);
    }


    public List<RecordDescriptionDto> getAllDescriptions(){
        List<RecordCategory> recordCategories = recordCategoryRepo.findAll();

        List<RecordDescriptionDto> descriptionsDtos = recordCategories.stream()
                .map(recordCategory -> recordDescriptionMapper.recordCategoryToDto(recordCategory))
                .collect(Collectors.toList());

        return descriptionsDtos;
    }

    public CompareVersionsDto getCompareVersionsByCriteria(RecordCompareVersionsCriteria criteria){
        if(criteria.listId != null &&criteria.idVersion1 != null && criteria.idVersion2 != null){
            return getCompareVersionsCriteria(criteria.listId, Math.min(criteria.idVersion1, criteria.idVersion2),
                    Math.max(criteria.idVersion1, criteria.idVersion2));
        }
        else if(criteria.listId != null && criteria.date1 != null && criteria.date2 != null){
            if(criteria.date2.isAfter(criteria.date1)){
                return getCompareVersionsCriteria(criteria.listId, getVersionByDate(criteria.listId, criteria.date1),
                        getVersionByDate(criteria.listId, criteria.date2));
            }else{
                return getCompareVersionsCriteria(criteria.listId, getVersionByDate(criteria.listId, criteria.date2),
                        getVersionByDate(criteria.listId, criteria.date1));
            }
        }
        throw new ReglissException(ErrorCode.COMPARE_VERSIONS_SELECT_VALID_CRITERIA);
    }

    public VersionsByDateDto getVersionsByDate(LocalDate date1,LocalDate date2, Long listId){
        VersionsByDateDto dto = new VersionsByDateDto();
        dto.versionId1 = getVersionByDate(listId, date1);
        dto.versionId2 = getVersionByDate(listId, date2);

        if(dto.versionId1 == dto.versionId2)
            throw new ReglissException(ErrorCode.COMPARE_VERSIONS_SAME_VERSION);

        return dto;
    }

    public CompareVersionsDto getCompareVersions(Long idVersion1, Long idVersion2, Long listId){
        if(idVersion1.equals(idVersion2))
            throw new ReglissException(ErrorCode.COMPARE_VERSIONS_SELECT_DIFFERENT_VERSIONS);

        Version version1 = versionRepo.findById(idVersion1).orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        Version version2 = versionRepo.findById(idVersion2).orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        CompareVersionsDto dto = new CompareVersionsDto();
        dto.addedRecordsBetweenVersions = recordRepo.getAddedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.modifiedRecordsBetweenVersions = recordRepo.getModifiedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.deletedRecordsBetweenVersions = recordRepo.getDeletedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.totalNumberVersion1 = version1.getComputedRecordNumbers().getTotal();
        dto.totalNumberVersion2 = version2.getComputedRecordNumbers().getTotal();
        dto.versionNumber1 = version1.getVersionNumber();
        dto.versionNumber2 = version2.getVersionNumber();
        dto.versionId1 = version1.getId();
        dto.versionId2 = version2.getId();
        Map<Long,Long> versions = versionRepo.getAllVersionNumbersByListId(listId).stream().collect(Collectors.toMap(ListVersionVO::getId,
                ListVersionVO::getVersionNumber));
        dto.addedRecordsBetweenVersions.forEach(r->getFromAndToVersionNumber(r,versions));
        dto.modifiedRecordsBetweenVersions.forEach(r->getFromAndToVersionNumber(r,versions));
        dto.deletedRecordsBetweenVersions.forEach(r->getFromAndToVersionNumber(r,versions));

        return dto;
    }

    private CompareVersionsLine getFromAndToVersionNumber(CompareVersionsLine record,Map<Long,Long> versions) {
        record.fromVersionNumber = versions.get(record.getFromVersionId()).toString();
        Long toVersionNumber = versions.get(record.getToVersionId());
        record.toVersionNumber = toVersionNumber!=null ? versions.get(record.getToVersionId()).toString(): null;
        return record;
    }

    private CompareVersionsDto getCompareVersionsCriteria(Long listId, Long idVersion1, Long idVersion2){
        if(idVersion1.equals(idVersion2)){
            throw new ReglissException(ErrorCode.COMPARE_VERSIONS_SELECT_DIFFERENT_VERSIONS_OR_DATES);
        }
        Version version1 = versionRepo.findById(idVersion1).orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        Version version2 = versionRepo.findById(idVersion2).orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        CompareVersionsDto dto = new CompareVersionsDto();
        dto.addedRecordsBetweenVersions = recordRepo.getAddedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.modifiedRecordsBetweenVersions = recordRepo.getModifiedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.deletedRecordsBetweenVersions = recordRepo.getDeletedRecordsBetweenVersions(idVersion1, idVersion2, listId).stream()
                .map(CompareVersionsLine::new ).collect(Collectors.toList());
        dto.totalNumberVersion1 = version1.getComputedRecordNumbers().getTotal();
        dto.totalNumberVersion2 = version2.getComputedRecordNumbers().getTotal();
        dto.versionNumber1 = version1.getVersionNumber();
        dto.versionNumber2 = version2.getVersionNumber();
        dto.versionId1 = version1.getId();
        dto.versionId2 = version2.getId();
        return dto;
    }

    private Long getVersionByDate(Long listId, LocalDate date){
        return versionRepo.getVersionByDate(listId, date)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND,"No version was found for date " + date.toString()));
    }

    public String getPreviousUuid(Long previousVersionId, String uuid){
        Optional<Record> record = recordRepo.findByExternalUuid(uuid);
        if(!record.isPresent()){
            throw new ReglissException();
        }
        record=recordRepo.findByExternalReferenceAndVersionId(record.get().getExternalReference(),previousVersionId);
        if(!record.isPresent()){
            throw new ReglissException();
        }
        return record.get().getExternalUuid();
    }
}
