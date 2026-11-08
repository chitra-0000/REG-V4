package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.Description3;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.SanctionType;
import com.bnpp.regliss.entity.six.entity.*;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.exporter.Description1FilterValues;
import com.bnpp.regliss.facade.dto.AllListsIncludedInConcatenatedList;
import com.bnpp.regliss.facade.dto.ListBreadcrumbDto;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.facade.dto.record.ProgramDto;
import com.bnpp.regliss.facade.mapper.*;
import com.bnpp.regliss.six.dto.*;
import com.bnpp.regliss.six.importer.mapper.SixFiltersMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.repository.six.*;
import com.bnpp.regliss.search.ReglissListSearchCriteria;
import com.bnpp.regliss.search.ReglissListSearchResult;
import com.bnpp.regliss.search.ReglissListSearchResultVersion;
import com.bnpp.regliss.service.*;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.CurrentVersionPermissions;
import com.bnpp.regliss.service.permission.ListPermissionRequest;
import com.bnpp.regliss.service.permission.VersionPermissionRequest;
import com.bnpp.regliss.vo.ListBreadcrumbVO;
import com.bnpp.regliss.vo.ListImportExportVO;
import com.bnpp.regliss.vo.ListVersionVO;
import com.bnpp.regliss.vo.MappingColumnVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.Column;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bnpp.regliss.entity.ImportFileType.*;
import static com.bnpp.regliss.exporter.Description1FilterValues.RCA;RCAA;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Facade
@ReglissWebProfile
public class ReglissListFacade {

    private static final String PEP = "Politically Exposed Person (PEP)";
    private static final String SIP = "Special Interest Person (SIP)";
    private static final String SIE = "Special Interest Entity (SIE)";
    private static final String RCA = "Relative or Close Associate (RCA)";
    private static final String RCAA = "Relative or Close Associate - All (RCAA)";
    private static final String OTH = "OTH";
    private static final String CREATE = "Create";
    private static final String DELETE = "Delete";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReglissListFacade.class);

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private ReglissListMapper mapper;

    @Autowired
    private ReglissListAuditService listAuditService;

    @Autowired
    private VersionRepository versionRepo;

    @Autowired
    private ProgramRepository programRepo;

    @Autowired
    private BatchExportService batchExportService;

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private CurrentListPermissions listPermissions;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private UserPermissionsRepository userPermissionsRepo;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private DJExportFilterRepository filterRepo;

    @Autowired
    private DJExportSubfilterRepository subfilterRepository;

    @Autowired
    private DJExportFilterMapper djExportFilterMapper;
    @Autowired
    private CurrentListPermissions listPermissionService;
    @Autowired
    private CurrentVersionPermissions currentVersionPermissions;
    @Autowired
    private VersionLockService versionLockService;
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private ReglissListImportExportMapper importExportMapper;

    @Autowired
    private ReglissListCharacteristicsMapper listCharacteristicsMapper;

    @Autowired
    private ConcatenatedListRepository concatenatedlistRepo;

    @Autowired
    private ConcatenatedListMapper concatenatedListMapper;

    @Autowired
    private AuditRepository auditRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuditService userAuditService;

    @Autowired
    private SixFiltersRepository sixfiltersRepo;

    @Autowired
    private SixSubFiltersRepository sixSubFiltersRepository;

    @Autowired
    private SixSubORFiltersRepository sixSubORFiltersRepository;

    @Autowired
    private SixFiltersMapper sixFiltersMapper;

    @Autowired
    private SixFieldTypeFilterRepository sixFieldTypeFilterRepository;

    @Autowired
    private SixTargetRepository sixTargetRepository;

    private static final java.util.regex.Pattern CAMEL_SPLIT =
            java.util.regex.Pattern.compile("(?<=[a-z])(?=[A-Z])");

    public ReglissListDto getList(long listId) {
        ReglissList list = listRepo.findById(listId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        ReglissListDto listDto = mapper.toDetailsDto(list);
        versionMapper.fillVersionsLockDetails(listDto.versions);
        listAuditService.auditGet(list);
        return listDto;
    }

    public ReglissListImportExportDto getListImportExport(long listId){
        ReglissList reglissList = this.listRepo.getExactlyOne(listId);
        Optional<ListImportExportVO> list=this.listRepo.getListImportExportById(listId);
        if(list.isPresent()) {
            List<MappingColumnVO> mappingColumns = this.listRepo.getMappingColumnsById(listId);
            List<MappingColumnDto> mappingColumnsDtos = mappingColumns.stream().map(mc -> importExportMapper
                    .toReglissListImportExportMappingColumnDto(mc)).collect(Collectors.toList());
            return importExportMapper.toReglissListImportExportDto(list.get(), mappingColumnsDtos, reglissList);
        }else {
            return  importExportMapper.toReglissListImportWhenNoCsvFileExportDto(reglissList);
        }
    }

    public void updateListImportExport(ReglissListImportExportDto dto){
        ReglissList listInDb = this.listRepo.getExactlyOne(dto.id);

        ReglissList oldList = new ReglissList(listInDb);
        ReglissList newList = importExportMapper.toEntity(listInDb, dto);
        if (oldList.getConfidentiality().getPriority() < newList.getConfidentiality().getPriority()) {
            removeCustomPermissionsForAllUsers(oldList);
        }
        Optional<MappingFile> previousActiveMappingFileOpt = listInDb.getActiveMappingFile();
            if (previousActiveMappingFileOpt.isPresent()&& dto.mappingFile==null) {
                previousActiveMappingFileOpt.get().setDeleted(true);
            }
        newList = listRepo.save(newList);
        listAuditService.auditUpdate(oldList, newList);
        listAuditService.auditAutomaticImportFieldsChanged(oldList, newList);

    }


    public List<VersionForVersionPageDto> getVersionsForVersionPage(Long listId){
        List<ListVersionVO> list = versionRepo.getAllVersionForVersionPageByListId(listId);
        ReglissList listOfVersions = listRepo.getExactlyOne(listId);
        return list.stream().map(v -> versionMapper.toVersionForVersionPageDto(v, getNumberOfMinorUpdates(v,
                        listOfVersions.getImportFileType()), listOfVersions))
                            .sorted(comparing(VersionForVersionPageDto::getVersionNumber).reversed())
                            .collect(Collectors.toList());
    }

    public void regenerateFile(Long listId) { batchExportService.persistSixFilteredFileGenerationStubInTx(listId); }

    public boolean isSixRegenerationAvailable() { return batchExportService.isSixRegenerationAvailable(); }

    private Long getNumberOfMinorUpdates(ListVersionVO vo, ImportFileType importFileType){
        if(importFileType.isWatchlistFormat()) {
            return 0L;
        }
        return auditRepo.countAudits4MinorUpdates(vo.getId());
    }

    public ListBreadcrumbDto getBreadcrumbForList(Long listId){
        ListBreadcrumbVO list=listRepo.getListBreadcrumb(listId);
        ListBreadcrumbDto dto=mapper.toListBreadcrumbDto(list);
        List<UserListPermission> allPermissionsForList = userPermissionsRepo.findByUserAndReglissListId(requestContext.getCurrentUser(),
                listId);
        if(!CollectionUtils.isEmpty(allPermissionsForList)) {
            dto.canDownloadExport = allPermissionsForList.stream().filter(p -> p.getPermission()
                    .equals(ListPermission.DOWNLOADEXPORT)).count() > 0;
            dto.canDownloadImport = allPermissionsForList.stream().filter(p -> p.getPermission()
                    .equals(ListPermission.DOWNLOADIMPORT)).count() > 0;
        }

        return dto;
    }

    public List<ReglissListShortNameDto> getAllListShortNamesAndIds() {
        List<ListShortNameDto> listShortNames = listRepo.getAllListShortNames();
        return listShortNames.stream().map(listShortNameDto -> new ReglissListShortNameDto(listShortNameDto.getReference()
                + "-" + listShortNameDto.getShortName(),
                listShortNameDto.getId())).collect(Collectors.toList());
    }




    public long create(ReglissListDto dto) {
        ReglissList newList = mapper.toEntity(new ReglissList(), dto);

        if (listRepo.countByReferenceAndDeletedFalse(newList.getReference()) != 0) {
            throw new ReglissException(ErrorCode.LIST_REFERENCE_ALREADY_IN_USE);
        }

        if (listRepo.countByShortNameIgnoreCaseAndDeletedFalse(newList.getShortName()) != 0) {
            throw new ReglissException(ErrorCode.LIST_SHORTNAME_ALREADY_IN_USE);
        }

        if (listRepo.countByNameIgnoreCaseAndDeletedFalse(newList.getName()) != 0) {
            throw new ReglissException(ErrorCode.LIST_NAME_ALREADY_IN_USE);
        }
        listRepo.save(newList);
        listAuditService.auditCreate(newList);
        return newList.getId();
    }

    public void updateBroadcastEmails(Long listId, BroadcastDto broadcastDto) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        mapper.mapBroadcastEmails(listInDb, broadcastDto.emails);
        listInDb.setCommentEn(broadcastDto.commentEn);
        listInDb.setCommentFr(broadcastDto.commentFr);
        ReglissList newList = listRepo.save(listInDb);
        listAuditService.auditUpdate(oldList, newList);
    }

    public BroadcastDto getBroadcastEmails(Long listId){
        ReglissList list = listRepo.findById(listId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        return  mapper.toBroadcastDto(list);
    }

    public void update(ReglissListDto dto) {
        if (dto.isGlobalPlusOfac && dto.importConfiguration.allowAutoUpload) {
            checkUpdateOfacAutomaticList(dto.importConfiguration);
        }
        ReglissList listInDb = listRepo.getExactlyOne(dto.id);

        if (this.doesNewRefExist(listInDb, dto.reference)) {
            throw new ReglissException(ErrorCode.LIST_REFERENCE_ALREADY_IN_USE);
        }

        ReglissList oldList = new ReglissList(listInDb);
        ReglissList newList = mapper.toEntity(listInDb, dto);

        if (oldList.getConfidentiality().getPriority() < newList.getConfidentiality().getPriority()) {
            removeCustomPermissionsForAllUsers(oldList);
        }
        boolean wantToDeactivate = oldList.isActive() && !newList.isActive();
        if (wantToDeactivate && !newList.canBeDeactivated()) {
            throw new ReglissException(ErrorCode.LIST_CANNOT_DEACTIVATE_VERSIONS_ONGOING);
        }
        newList = listRepo.save(newList);
        listAuditService.auditUpdate(oldList, newList);
        if (oldList.getConfidentiality().getPriority()!= newList.getConfidentiality().getPriority()
                || !oldList.getCountry().getId().equals(newList.getCountry().getId())
                || !oldList.getScope().name().equals(newList.getScope().name())) {
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_MOA, true);
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_CG, true);
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_CL, true);
        }
    }

    private void checkUpdateOfacAutomaticList(ImportConfigurationDto dto) {
        if (dto.importFileType != DOW_JONES_CORE && dto.importFileType != CUSTOM) {
            throw new ReglissException(ErrorCode.LIST_AUTOMATIC_AND_NON_CORE_CANNOT_BE_OFAC,
                    referenceService.translateEnum(dto.importFileType));
        }
    }

    public void delete(long id) {
        ReglissList list = listRepo.getExactlyOne(id);
        list.setDeleted(true);
        listAuditService.auditDeletion(list);
    }

    public Boolean checkIfCRBListExists(Long id) {
        Optional<Long> countCRBLists = listRepo.getCountCRBLists(id);
        return countCRBLists.isPresent() && countCRBLists.get() > 0;
    }

    public List<ReglissListSearchResult> searchReglissListsOptimized(ReglissListSearchCriteria criteria) {
        listAuditService.logSearch(criteria);

        criteria.authenticatedLowLevel = !requestContext.isHighAuthentication();

        Set<Long> allowedListsIdsForUser = listPermissions.getAllowedListsIdsForCurrentUser();
        List<ReglissListSearchResult> foundLists = listRepo.searchOptimized(criteria, allowedListsIdsForUser);

        for (ReglissListSearchResult list : foundLists) {
            list.setReadOnly(listPermissionService.isListReadOnly());
            list.setVersionReadOnly(listPermissionService.isVersionReadOnly(new ListPermissionRequest(list)));
            postProcessVersion(list.getApplicableVersion(), list.getCurrentVersion());
        }

        List<ReglissListSearchResultVersion> allVersionsDisplayed = foundLists
                .stream()
                .flatMap(ReglissListSearchResult::getAllVersionsDisplayedStream)
                .collect(toList());
        fillVersionsLockDetailsOptimized(allVersionsDisplayed);

        return foundLists;
    }

    public ReglissListCharacteristicsDto getListCharacteristicsById(Long id) {
        ListCharacteristicsDto list = this.listRepo.getListCharacteristicsById(id)
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
        return  listCharacteristicsMapper.toListCharacteristicsDto(list);
    }

    public Long createListCharacteristics(ReglissListCharacteristicsDto dto) {
        ReglissList newList = listCharacteristicsMapper.toEntity(new ReglissList(), dto);

        if (listRepo.countByReferenceAndDeletedFalse(newList.getReference()) != 0) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_REFERENCE_ALREADY_IN_USE);
        }

        if (listRepo.countByShortNameIgnoreCaseAndDeletedFalse(newList.getShortName()) != 0) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_SHORTNAME_ALREADY_IN_USE);
        }

        if (listRepo.countByNameIgnoreCaseAndDeletedFalse(newList.getName()) != 0) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_NAME_ALREADY_IN_USE);
        }

        listRepo.save(newList);
        listAuditService.auditCreate(newList);
        assignUserDownloadPermissionForList(newList,Profile.ADMIN_MOA, false);
        assignUserDownloadPermissionForList(newList,Profile.ADMIN_CG, false);
        assignUserDownloadPermissionForList(newList,Profile.ADMIN_CL, false);
        return newList.getId();
    }
    private void assignUserDownloadPermissionForList(ReglissList reglissList,Profile profile,boolean isUpdated) {
        List<User> listUser=userRepository.findByProfile(profile);
        for(User user:listUser) {
            User oldUser = new User(user);
            Set<UserListPermission> newPermissions=new HashSet<>();
            boolean hasReadPermissionForList=listPermissions.hasReadPermissionForList(user,new ListPermissionRequest(reglissList));
            if(user.hasExportedFileDownloadPermissionNow() && hasReadPermissionForList) {
                UserListPermission userGrant = new UserListPermission();
                userGrant.setPermission(ListPermission.DOWNLOADEXPORT);
                userGrant.setReglissListId(reglissList.getId());
                newPermissions.add(userGrant);
            }
            if(user.hasImportedFileDownloadPermissionNow() && hasReadPermissionForList) {
                UserListPermission userGrant = new UserListPermission();
                userGrant.setPermission(ListPermission.DOWNLOADIMPORT);
                userGrant.setReglissListId(reglissList.getId());
                newPermissions.add(userGrant);
            }

                if(!user.getAssignedListPermissions().isEmpty()) {
                    if(isUpdated) {
                        newPermissions.addAll(user.getAssignedListPermissions().stream()
                                .filter(perm ->!perm.getReglissListId().equals(reglissList.getId())).collect(toList()));
                    }else {
                        newPermissions.addAll(user.getAssignedListPermissions().stream().collect(toList()));
                    }
                }
                user.setAssignedListPermissions(newPermissions);
                userRepository.save(user);
                userAuditService.auditUpdate(oldUser,user);
        }

    }
    public void updateListCharacteristics(ReglissListCharacteristicsDto dto) {
        ReglissList listInDb = this.listRepo.getExactlyOne(dto.id);
        if (this.doesNewRefExist(dto.id, dto.reference)) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_REFERENCE_ALREADY_IN_USE);
        }
        ReglissList oldList = new ReglissList(listInDb);
        ReglissList newList = listCharacteristicsMapper.toEntity(listInDb, dto);
        if (oldList.getConfidentiality().getPriority() < newList.getConfidentiality().getPriority()) {
            removeCustomPermissionsForAllUsers(oldList);
        }
        boolean wantToDeactivate = oldList.isActive() && !newList.isActive();
        if (wantToDeactivate && !newList.canBeDeactivated()) {
            throw new ReglissException(ReglissException.ErrorCode.LIST_CANNOT_DEACTIVATE_VERSIONS_ONGOING);
        }
        newList = listRepo.save(newList);
        listAuditService.auditUpdate(oldList, newList);
        if (oldList.getConfidentiality().getPriority()!= newList.getConfidentiality().getPriority()
                || !oldList.getCountry().getId().equals(newList.getCountry().getId())
                || !oldList.getScope().name().equals(newList.getScope().name())) {
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_MOA, true);
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_CG, true);
            assignUserDownloadPermissionForList(newList,Profile.ADMIN_CL, true);
        }
    }

    private void fillVersionsLockDetailsOptimized(List<ReglissListSearchResultVersion> versionDtos) {
        Set<Long> versionIds = versionDtos.stream().map(ReglissListSearchResultVersion::getId).collect(toSet());
        if (versionIds.isEmpty()) {
            return;
        }
        Map<Long, VersionLock> allLocks = versionLockService.getLastActiveLocks(versionIds);
        for (ReglissListSearchResultVersion versionColumnDto : versionDtos) {
            VersionLock versionLock = allLocks.get(versionColumnDto.getId());
            if (versionLock != null && !versionLock.isOwnedBy(requestContext.getUserId())) {
                versionColumnDto.setLockedBy(versionLock.getUser().getUsername());
            }
        }
    }


    private void postProcessVersion(ReglissListSearchResultVersion... versionColumnDTOs) {
        Arrays.stream(versionColumnDTOs).filter(Objects::nonNull).forEach(versionColumnDTO -> {
            versionColumnDTO.setStatus(versionMapper.translateVersionStatus(versionColumnDTO.getStatusValue(),
                    versionColumnDTO.isWasAutoUploaded()));
            versionColumnDTO.setReadOnly(currentVersionPermissions.isVersionReadOnlyForDisplayInList(new VersionPermissionRequest(
                    versionColumnDTO)));
        });
    }

    public List<ProgramDto> getAvailableProgramsForReglementary(Long idList) {
        return programRepo.getAllForListWithRecordCount(idList).stream()
                .map(arr -> new ProgramDto((Program) arr[0], (long) arr[1]))
                .sorted(comparing(ProgramDto::getLabel, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(toList());
    }

    public void setImportParameters(Long idList, ImportConfigurationDto importConfigurationDto) {
        ImportFileType importFileType = importConfigurationDto.importFileType;

        checkImportConfiguration(importFileType,
                importConfigurationDto.allowAutoUpload,
                importConfigurationDto.allowManualUpload);
        if (importFileType == DOW_JONES_AME) {
            checkListFormatAlreadyExists(idList, importFileType);
        }

        ReglissList newList = listRepo.getExactlyOne(idList);
        if (newList.isGlobalPlusOfac()) {
            checkUpdateOfacAutomaticList(importConfigurationDto);
        }
        if (importFileType == DOW_JONES_WATCHLIST) {
            newList.setFircosoftFormat(false);
            newList.setSafewatchFormat(false);
            newList.setVigilanceFormat(false);
            newList.setGenericFormat(false);
        }
        ReglissList oldList = new ReglissList(newList);

        newList.setImportConfiguration(mapper.toImportConfigurationEntity(importConfigurationDto));
        Optional<MappingFile> oldMappingFileOpt = newList.getActiveMappingFile();
        oldMappingFileOpt.ifPresent(mappingFile -> mappingFile.setDeleted(true));

        listAuditService.auditAutomaticImportFieldsChanged(oldList, newList);
    }

    public List<WatchlistDescriptionAssociationDto> getAllDescriptionsCombinations(ImportFileType format) {
        LOGGER.debug("{}",format);
        List<WatchlistDescriptionAssociationDto> descriptionsDtos = new ArrayList<>();
        List<RecordCategory> recordCategories = recordCategoryRepo.findAll();
        RecordCategory rcaCat = recordCategoryRepo.findByLabel(RCA);
        RecordCategory rcaaCat = recordCategoryRepo.findByLabel(RCAA);

        for (RecordCategory recordCategory : recordCategories) {
            List<Description1FilterValues> description1FilterValues = getDescription1FilterValuesFromCategory(recordCategory);

            for (Description1FilterValues desc1Value : description1FilterValues) {
                Set<RecordCategory> categoriesToProcess = new HashSet<>();
                categoriesToProcess.add(recordCategory);
                if (desc1Value.includeRca() || desc1Value.includeRcaa()) {
                    if (rcaCat != null) categoriesToProcess.add(rcaCat);
                    if (rcaaCat != null) categoriesToProcess.add(rcaaCat);
                }

                for (RecordCategory cat : categoriesToProcess) {
                    if (cat.getSanctionTypes() == null || cat.getSanctionTypes().isEmpty()) {
                        getSanctionTypesIsEmpty(Collections.singletonList(desc1Value), descriptionsDtos, cat);
                    } else {
                        getSanctionTypesIsNotEmpty(Collections.singletonList(desc1Value), descriptionsDtos, cat);
                    }
                }
            }
        }
        return descriptionsDtos;
    }

    private void getSanctionTypesIsNotEmpty(List<Description1FilterValues> description1FilterValues,
                                            List<WatchlistDescriptionAssociationDto> descriptionsDtos, RecordCategory recordCategory) {
        for (SanctionType sanctionType : recordCategory.getSanctionTypes()) {
            if (sanctionType.getDescriptions3().isEmpty()) {
                for (Description1FilterValues description1FilterValue : description1FilterValues) {
                    descriptionsDtos.add(new WatchlistDescriptionAssociationDto(description1FilterValue,
                            recordCategory.isPEP(), sanctionType.getId(), null));
                }
            } else {
                for (Description3 description3 : sanctionType.getDescriptions3()) {
                    for (Description1FilterValues description1FilterValue : description1FilterValues) {
                        descriptionsDtos.add(new WatchlistDescriptionAssociationDto(description1FilterValue,
                                recordCategory.isPEP(), sanctionType.getId(), description3.getId()));
                    }
                }
            }
        }
    }

    private void getSanctionTypesIsEmpty(List<Description1FilterValues> description1FilterValues,
                                         List<WatchlistDescriptionAssociationDto> descriptionsDtos, RecordCategory recordCategory) {
        for (Description1FilterValues description1FilterValue : description1FilterValues) {
            descriptionsDtos.add(new WatchlistDescriptionAssociationDto(description1FilterValue,
                    recordCategory.isPEP(), null, null));
        }
    }

    private List<Description1FilterValues> getDescription1FilterValuesFromCategory(RecordCategory recordCategory) {
        List<Description1FilterValues> description1FilterValues = new ArrayList<>();
        String label = recordCategory.getLabel();

        if (!OTH.equals(label) && !recordCategory.hasBusinessConcept(ReferentialBusinessConcept.DEFAULT_VALUE)) {
            if (label.equals(PEP) || label.equals(SIP) || label.equals(SIE)) {
                description1FilterValues.add(Description1FilterValues.fromLabelEn(label));
                description1FilterValues.add(Description1FilterValues.fromLabelEnAndIncludeRca(label, true, true));
            }
            else if (label.equals(RCA) || label.equals(RCAA)) {
                description1FilterValues.add(RCA_RCAA);
            }
        }
        return description1FilterValues;
    }

    public List<ReglissListUniqueFieldsDto> getUniqueFields() {
        return listRepo.findByDeletedFalse().stream().map(mapper::toUniqueFieldsDto).collect(toList());
    }

    public List<ImportTypeDto> getImportTypes() {
        List<ImportFileType> importFileTypes= Stream.of(ImportFileType.values()).collect(toList());
        List<ImportTypeDto> result;
        if(this.checkListFormatAlreadyExists(DOW_JONES_AME)){
            importFileTypes = importFileTypes.stream().filter(f->f.isAmeFormat()).collect(toList());
        }
        if(this.checkListFormatAlreadyExists(DOW_JONES_WATCHLIST)){
            result = importFileTypes.stream().filter(f->!f.isWatchlistFormat()).map(this::enumToStringRef).collect(toList());
        }
        else{
            result=importFileTypes.stream().filter(f->!f.isExtraWatchlistFormat()).map(this::enumToStringRef).collect(toList());
        }
        return result;

    }

    private ImportTypeDto enumToStringRef(ImportFileType enumValue) {
        ImportTypeDto ref = new ImportTypeDto();
        ref.id = enumValue.toString();
        ref.label = referenceService.translateEnum(enumValue);
        return ref;
    }

    private void checkImportConfiguration(ImportFileType importFileType,
                                          boolean allowAutoUpload,
                                          boolean allowManualupload) {
        switch (importFileType) {
            case DOW_JONES_AME:
            case DOW_JONES_CORE:
            case DOW_JONES_WATCHLIST:
            case DOW_JONES_WATCHLIST_EXTRA:
                if (!allowAutoUpload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "Dow Jones import configuration must allow auto upload");
                }
                if (allowManualupload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "Dow Jones import configuration must not allow manual upload");
                }
                break;

            case MANUAL_PROCESSING:
                if (allowAutoUpload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "Manual import configuration must not allow auto upload");
                }
                if (!allowManualupload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "Manual import configuration must allow manual upload");
                }
                break;

            case NOT_SUPPORTED:
                if (allowAutoUpload || allowManualupload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "UI created list import configuration must not allow any type of upload");
                }
                break;

            case CUSTOM:
                if (!allowAutoUpload && !allowManualupload) {
                    throw new ReglissException(ErrorCode.GENERAL,
                            "Custom list import configuration must allow at least one type of upload");
                }
                break;

            default:
                throw new ReglissException(ErrorCode.GENERAL, "Unchecked import file type provided");
        }
    }

    private void checkListFormatAlreadyExists(long idList, ImportFileType format) {
        if (listRepo.countOtherAutomaticActiveListsByFormat(idList, format) != 0) {
            throw new ReglissException(ErrorCode.LIST_WITH_IMPORT_FILE_TYPE_ALREADY_EXISTS, referenceService.translateEnum(format));
        }
    }

    private boolean checkListFormatAlreadyExists(ImportFileType format) {
        return listRepo.countActiveAndAutomaticListsByFormat(format) != 0;
    }

    private void removeCustomPermissionsForAllUsers(ReglissList oldList) {
        userPermissionsRepo.deleteByReglissList(oldList.getId());
    }

    public DJExportFilterAndSubfiltersDto getFilter(Long filterId) {
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        return djExportFilterMapper.toDJExportFilterAndSubfiltersDto(filter);
    }

    public List<DJExportFilterDto> getFilters(Long listId) {
        Comparator<DJExportFilterDto> filterActiveComparator= Comparator.comparing(filter->filter.isActive,
                (a1,a2)->{return Boolean.compare(a1,a2);});
        List<DJExportFilter> filters = filterRepo.getFiltersByListIdAndDeletedFalse(listId);
        return filters.stream().map(djExportFilterMapper::toDJExportFilterDto).sorted(filterActiveComparator.reversed())
                .collect(Collectors.toList());
    }


    public Long createExportFilter(Long listId, DJExportFilterDto dto) {

        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        validateFilterTagIsUniqueAndNotNull(dto.filterTag, listInDb);
        DJExportFilter newFilter = filterRepo.save(djExportFilterMapper.toDJExportFilter(new DJExportFilter(), dto, listInDb));
        listInDb.addFilter(newFilter);
        if (listInDb.isFircosoftFormat()) {
            newFilter.addExtension(FileExportExtension.zip);
        }
        listAuditService.auditUpdate(oldList, listInDb);
        return newFilter.getId();
    }


    public void updateExportFilter(Long listId, DJExportFilterDto dto) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        DJExportFilter filter = filterRepo.getExactlyOne(dto.id);
        if (!filter.getFilterTag().equals(dto.filterTag)) {
            validateFilterTagIsUniqueAndNotNull(dto.filterTag, listInDb);
        }
        djExportFilterMapper.toDJExportFilter(filter, dto, listInDb);
        listAuditService.auditFilterUpdate(oldList, listInDb, dto.filterName);
    }

    private void validateFilterTagIsUniqueAndNotNull(String filterTag, ReglissList listInDb) {
        if (StringUtils.isEmpty(filterTag)) {
            throw new ReglissException(ErrorCode.FILTER_TAG_IS_REQUIERED);
        }
        if (!listInDb.getImportFileType().isSIXMainFileType() && filterRepo.existsDJExportFilterByFilterTagAndDeletedFalse(filterTag)) {
            throw new ReglissException(ErrorCode.FILTER_TAG_ALREADY_IN_USE);
        }
        if (listInDb.getImportFileType().isSIXMainFileType() && sixfiltersRepo.existsSixFiltersByFilterTagAndDeletedFalse(filterTag)) {
            throw new ReglissException(ErrorCode.FILTER_TAG_ALREADY_IN_USE);
        }
    }

    public void updateExportSubfilters(Long listId, Long filterId, List<DJExportSubfilterDto> subfilterDtos) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);

        Set<DJExportSubfilter> newSubfilters = subfilterDtos.stream()
                .sorted(comparing(dto -> dto.id, nullsLast(naturalOrder())))
                .map(djExportFilterMapper::toDJExportSubfilter)
                .collect(Collectors.toSet());


        EntityCollectionDiff<DJExportSubfilter> subfilterDiff = new EntityCollectionDiff<>(filter.getActiveSubfilters(),
                newSubfilters, e -> Collections.singletonList(e.getId()));

        for (DJExportSubfilter toDelete : subfilterDiff.getDeletedElements()) {
            filter.removeSubfilter(toDelete);
        }

        for (DJExportSubfilter toCreate : subfilterDiff.getCreatedElements()) {
            filter.addSubfilter(toCreate);
        }
        listAuditService.auditSubfilters(listInDb, subfilterDiff, filter.getFilterName());
    }

    public DJExportFilterAndSubfiltersDto updateExportSubfilter(Long listId, Long filterId, Long subfilterId,
                                                                DJExportSubfilterDto subfilterDto) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        DJExportSubfilter subfilter = subfilterRepository.getExactlyOne(subfilterId);
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);

        DJExportSubfilter updatedAuditSubfilter = djExportFilterMapper.toDJExportSubfilter(subfilterDto);
        listAuditService.auditSubfilterUpdate(subfilter, updatedAuditSubfilter,oldList, subfilterDto.subfilterName);
        DJExportSubfilter updatedSubfilter = djExportFilterMapper.toDJExportSubfilter(subfilterDto, subfilter);
        filter.updateSubfilter(updatedSubfilter);



        return this.djExportFilterMapper.toDJExportFilterAndSubfiltersDto(filter);
    }


    public DJExportFilterAndSubfiltersDto createExportSubfilter(Long listId,Long filterId, DJExportSubfilterDto subfilterDto) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        Set<DJExportSubfilter> oldSubfilters = filter.getSubfilters();
        Set<DJExportSubfilter> newSubfilters = new HashSet<>();

        DJExportSubfilter updatedSubfilter = djExportFilterMapper.toDJExportSubfilter(subfilterDto);
        newSubfilters.add(updatedSubfilter);

        EntityCollectionDiff<DJExportSubfilter> subfilterDiff = new EntityCollectionDiff<>(oldSubfilters,
                newSubfilters, e -> Collections.singletonList(e.getId()));

        listAuditService.auditAddDeleteSubfilters(oldList, subfilterDiff,subfilterDto.subfilterName, CREATE);
        filter.addSubfilter(updatedSubfilter);
        return this.djExportFilterMapper.toDJExportFilterAndSubfiltersDto(filter);
    }

    public DJExportFilterAndSubfiltersDto deleteExportSubfilter(Long listId,Long filterId, Long subfilterId) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        DJExportSubfilter subfilter = subfilterRepository.getExactlyOne(subfilterId);
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);

        Set<DJExportSubfilter> oldSubfilters = filter.getSubfilters().stream().filter(e->!e.getId().equals(subfilter.getId()))
                .collect(toSet());

        filter.removeSubfilter(subfilter);

        EntityCollectionDiff<DJExportSubfilter> subfilterDiff = new EntityCollectionDiff<>(filter.getSubfilters(), oldSubfilters,
                e -> Collections.singletonList(e.getId()));

        listAuditService.auditAddDeleteSubfilters(oldList, subfilterDiff, subfilter.getSubfilterName(), DELETE);

        return this.djExportFilterMapper.toDJExportFilterAndSubfiltersDto(filter);
    }

    public  DJExportSubfilterDto readExportSubfilter(Long subfilterId) {

        DJExportSubfilter subfilter = subfilterRepository.getExactlyOne(subfilterId);

        return this.djExportFilterMapper.toDJExportSubfilterDto(subfilter);
    }


    public void activateFilter(Long filterId) {
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        filter.setActive(true);
        listAuditService.auditFilterActivation(filter);
    }

    public void deactivateFilter(Long filterId) {
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        filter.setActive(false);
        listAuditService.auditFilterDeactivation(filter);
    }

    public void deleteFilter(Long filterId) {
        DJExportFilter filter = filterRepo.getExactlyOne(filterId);
        filter.setDeleted(true);
        listAuditService.auditFilterDeletion(filter);
    }

    public boolean doesNewRefExist(Long listId, String ref) {
        ReglissList list = (listId != null) ? listRepo.getExactlyOne(listId) : null;
        return this.doesNewRefExist(list, ref);
    }

    private boolean doesNewRefExist(ReglissList list, String ref) {
        return ref != null && (list == null || Integer.parseInt(ref) != Integer.parseInt(list.getReference())) &&
                !listRepo.findByRefAsNumber(Integer.parseInt(ref)).isEmpty();
    }

    public void updateListToConcatenatedList(Long listId, List<Long> concatenatedListsId) {
        ReglissList list = listRepo.findById(listId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));

        Set<ConcatenatedList> newConcatenatedListsForList = concatenatedlistRepo.findAllById(concatenatedListsId).stream().collect(toSet());
        if(!list.getConcatenatedLists().isEmpty()){
            list.getConcatenatedLists().stream()
                    .filter(concatenatedList -> !newConcatenatedListsForList.contains(concatenatedList))
                    .forEach(concatenatedList -> concatenatedList.removeList(list));
        }


        newConcatenatedListsForList.forEach(concatenatedList -> concatenatedList.addReglissList(list));
        concatenatedlistRepo.saveAll(concatenatedListsToSave(list.getConcatenatedLists(), newConcatenatedListsForList));
    }

    private Set<ConcatenatedList> concatenatedListsToSave(Set<ConcatenatedList> concListFromReglissList,
                                                          Set<ConcatenatedList> newConcatenatedListsForList){
        Set<ConcatenatedList> concatenatedListsToSave = new HashSet<>(newConcatenatedListsForList);
        concatenatedListsToSave.addAll(concListFromReglissList);
        return concatenatedListsToSave;
    }

    public List<ConcatenatedListDisplayDto> getConcatenatedListsForList(Long listId){
        ReglissList list = listRepo.findById(listId)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND));
        return list.getConcatenatedLists().stream()
                .map(clist -> new ConcatenatedListDisplayDto(clist.getId(), clist.getLabel(), clist.getPublicationDate(), clist.isActive(
                .collect(toList());
    }

    public List<ReglissListsForConcListsDto> getReglissListsExceptAmeAndWl(){
        List<ListForConcatenatedLists> reglissLists = listRepo.getAllNotAmeOrWl();
        return reglissLists.stream()
                .map(list -> new ReglissListsForConcListsDto(list.getId(), list.getReference(), list.getShortName(), list.getName()))
                .collect(toList());
    }


    public List<AllListsIncludedInConcatenatedList> getAllListsIncludedInConcatenatedList(Long id){
        ConcatenatedList concatenatedList = concatenatedlistRepo.getExactlyOne(id);
        List<Long> listsIds = concatenatedList.getListsIds();
        return listsIds.isEmpty() ? Collections.emptyList() : listRepo.getAllListsIncludedInConcatenatedList(listsIds).stream()
                .map(vo -> new AllListsIncludedInConcatenatedList(vo.getName(), vo.getLastPublicationDate()))
                .collect(toList());
    }

    public SixListsCountsDto checkIfSixListExists(Long id) {
        List<ReglissList> sixImportTypeLists = listRepo.getAllSixLists(id);
        SixListsCountsDto sixListCounts = new SixListsCountsDto();

        for (ReglissList list : sixImportTypeLists) {
            if (list.getImportFileType().isSIXINSTRUMENTSFileType()) {
                sixListCounts.sixInstructList = true;
            }
            if (list.getImportFileType().isSIXStructuredFileType()) {
                sixListCounts.sixStructList = true;
            }
            if (list.getImportFileType().isSIXOptionsFileType()) {
                sixListCounts.sixOptionsList = true;
            }
        }
        return sixListCounts;
    }

    public List<SixFiltersDto> getSixFilters(Long listId) {
        Comparator<SixFiltersDto> filterActiveComparator= Comparator.comparing(filter->filter.isActive, Boolean :: compare);
        List<SixFilters> filters = sixfiltersRepo.getFiltersByListIdAndDeletedFalse(listId);
        return filters.stream().map(sixFiltersMapper::toSixFiltersDto).sorted(filterActiveComparator.reversed()).collect(Collectors.toList());
    }

    public SixFiltersAndSubFiltersDto getSixFilter(Long filterId) {
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        return sixFiltersMapper.toSixFilterAndSubfiltersDto(filter);
    }

    public SixSubFiltersDto getSixSubfilter(Long subfilterId) {
        SixSubFilters subfilter = sixSubFiltersRepository.getExactlyOne(subfilterId);
        return this.sixFiltersMapper.toSixSubfilterDto(subfilter);
    }

    public Long createSixFilter(Long listId, SixFiltersDto dto) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        validateFilterTagIsUniqueAndNotNull(dto.filterTag, listInDb);
        SixFilters newFilter = sixfiltersRepo.save(sixFiltersMapper.toSixFilter(new SixFilters(), dto, listInDb));
        listInDb.addSixFilter(newFilter);
        listAuditService.auditUpdate(oldList, listInDb);
        return newFilter.getId();
    }

    public void updateSixFilter(Long listId, SixFiltersDto dto, Long filterId) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        if (!filter.getFilterTag().equals(dto.filterTag)) {
            validateFilterTagIsUniqueAndNotNull(dto.filterTag, listInDb);
        }
        sixFiltersMapper.toSixFilter(filter, dto, listInDb);
        listAuditService.auditFilterUpdate(oldList, listInDb, dto.filterName);
    }

    public void deleteSixFilter(Long filterId) {
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        filter.setDeleted(true);
        listAuditService.auditSixFilterDeletion(filter);
    }

    public void activateSixFilter(Long filterId) {
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        filter.setActive(true);
        listAuditService.auditSixFilterActivation(filter);
    }

    public void deactivateSixFilter(Long filterId) {
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        filter.setActive(false);
        listAuditService.auditSixFilterDeactivation(filter);
    }

    public List<SixTableFieldNamesDto> getSixTablesFieldNames() {
        List<SixTableFieldNames> tables = new ArrayList<>();
        Class[] classes = {InstrumentFile.class, StructuredFile.class, OptionsFile.class};
        List<String> regimes = sixTargetRepository.findAllDistinctRegimes();
        for(Class<T> clazz : classes) {
            SixTableFieldNames table = new SixTableFieldNames();
            table.setFilterType(CAMEL_SPLIT.matcher(clazz.getSimpleName()).replaceAll(" ").trim());
            table.setFieldNames(getFieldsNames(clazz, regimes));
            tables.add(table);
        }

        // SIX_FIELD_TYPE_FILTER: one entry per filter type, each field name only once, even if the
        // table contains the same FILTER_TYPE / FIELD_NAME pair more than once (rows added by hand,
        // script run twice...). Blank values are ignored; filter types stay sorted, field names keep
        // the order of the table.
        List<SixFieldTypeFilter> sixFieldTypeFilters = sixFieldTypeFilterRepository.findAllByOrderByFilterType();
        Map<String, LinkedHashSet<String>> fieldNamesByFilterType = sixFieldTypeFilters.stream()
                .filter(f -> f.getFilterType() != null && !f.getFilterType().trim().isEmpty())
                .filter(f -> f.getFieldName() != null && !f.getFieldName().trim().isEmpty())
                .collect(Collectors.groupingBy(f -> f.getFilterType().trim(), TreeMap::new,
                        Collectors.mapping(f -> f.getFieldName().trim(), Collectors.toCollection(LinkedHashSet::new))));

        for (Map.Entry<String, LinkedHashSet<String>> sixFieldTypeFilter : fieldNamesByFilterType.entrySet()) {
            SixTableFieldNames table1 = new SixTableFieldNames();
            table1.setFilterType(sixFieldTypeFilter.getKey());
            table1.setFieldNames(new ArrayList<>(sixFieldTypeFilter.getValue()));
            tables.add(table1);
        }
        return tables.stream().map(sixFiltersMapper :: toSixTableFieldNamesDto).collect(toList());
    }

    public List<String> getFieldsNames(Class<T> clazz, List<String> regimes) {
        List<String> fieldNames = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotation(Column.class).name()).filter(f -> !f.equals("ID") && !f.equals("LIST_ID")
                        && !f.equals("VERSION_ID") && !f.equals("REGIMES"))
                .collect(Collectors.toList());

        List<String> sixTargetFieldNames = Arrays.stream(SixTarget.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotation(Column.class).name()).filter(f -> !f.equals("ID") && !f.equals("SIX_INSTRU_ID")
                        && !f.equals("SIX_STRUCT_ID") && !f.equals("SIX_OPT_ID") && !f.equals("REGIME"))
                .collect(Collectors.toList());

        fieldNames.addAll(regimes.stream().map(regime -> regime + " - REGIME").collect(toList()));
        fieldNames.addAll(sixTargetFieldNames);
        return fieldNames.stream().sorted().collect(toList());
    }

    public SixFiltersAndSubFiltersDto createSixSubFilter(SixSubFiltersDto dto, Long listId, Long filterId) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);
        Set<SixSubFilters> oldSubfilters = filter.getSixSubFilters();
        Set<SixSubFilters> newSubfilters = new HashSet<>();

        SixSubFilters updatedSubfilter = sixFiltersMapper.toSixSubfilter(dto);
        newSubfilters.add(updatedSubfilter);

        EntityCollectionDiff<SixSubFilters> subfilterDiff = new EntityCollectionDiff<>(oldSubfilters,
                newSubfilters, e -> Collections.singletonList(e.getId()));

        listAuditService.auditAddDeleteSixSubfilters(oldList, subfilterDiff,dto.subFilterName, CREATE);
        filter.addSixSubfilter(updatedSubfilter);
        return this.sixFiltersMapper.toSixFilterAndSubfiltersDto(filter);
    }

    public SixFiltersAndSubFiltersDto updateSixSubFilter(SixSubFiltersDto dto, Long listId, Long filterId, Long subfilterId) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        SixSubFilters subfilter = sixSubFiltersRepository.getExactlyOne(subfilterId);
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);

        SixSubFilters updatedAuditSubfilter = sixFiltersMapper.toAuditSixSubfilter(dto);
        listAuditService.auditSixSubfilterUpdate(subfilter, updatedAuditSubfilter,oldList, dto.subFilterName);
        SixSubFilters updatedSubfilter = sixFiltersMapper.toSixSubfilter(dto, subfilter);
        filter.updateSixSubfilter(updatedSubfilter);

        return this.sixFiltersMapper.toSixFilterAndSubfiltersDto(filter);
    }

    public SixFiltersAndSubFiltersDto deleteSixSubFilter(Long listId, Long filterId, Long subfilterId) {
        ReglissList listInDb = listRepo.getExactlyOne(listId);
        ReglissList oldList = new ReglissList(listInDb);
        SixSubFilters subfilter = sixSubFiltersRepository.getExactlyOne(subfilterId);
        SixFilters filter = sixfiltersRepo.getExactlyOne(filterId);

        Set<SixSubFilters> oldSubfilters = filter.getSixSubFilters().stream().filter(e->!e.getId().equals(subfilter.getId()))
                .collect(toSet());
        filter.removeSixSubfilter(subfilter);

        sixSubORFiltersRepository.deleteSubFilterId(subfilterId);

        EntityCollectionDiff<SixSubFilters> subfilterDiff = new EntityCollectionDiff<>(filter.getSixSubFilters(), oldSubfilters,
                e -> Collections.singletonList(e.getId()));
        listAuditService.auditAddDeleteSixSubfilters(oldList, subfilterDiff, subfilter.getSixSubFilterName(), DELETE);

        return this.sixFiltersMapper.toSixFilterAndSubfiltersDto(filter);
    }
}

