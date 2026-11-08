package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;
import com.bnpp.regliss.facade.dto.ListBreadcrumbDto;
import com.bnpp.regliss.facade.dto.list.*;
import com.bnpp.regliss.six.importer.mapper.SixFiltersMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.EntityCollectionDiff;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.permission.CurrentListPermissions;
import com.bnpp.regliss.service.permission.ListPermissionRequest;
import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.vo.ListBreadcrumbVO;
import com.bnpp.regliss.vo.ListVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Service
@ReglissWebProfile
public class ReglissListMapper {

    @Autowired
    private ReferenceService reference;

    @Autowired
    private VersionMapper versionMapper;

    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private ReglissListRepository listRepository;

    @Autowired
    private MappingFileMapper mappingFileMapper;

    @Autowired
    private CurrentListPermissions listPermissionService;

    @Autowired
    private DJExportFilterMapper djExportFilterMapper;

    @Autowired
    private SixFiltersMapper sixFiltersMapper;

    public ReglissListUniqueFieldsDto toUniqueFieldsDto(ReglissList list) {
        ReglissListUniqueFieldsDto dto = new ReglissListUniqueFieldsDto();
        dto.id = list.getId();
        dto.shortName = list.getShortName();
        dto.name = list.getName();
        dto.reference = list.getReference();
        if (list.getImportConfiguration()!=null) {
            dto.djFileName = list.getImportConfiguration().getFileNamePrefix();
        }

        return dto;
    }

    public ReglissListDto toSearchResultDto(ReglissList list) {
        ReglissListDto dto = toDto(list);
        dto.applicableVersion = list.getLastApplicableVersion().map(v->versionMapper.toListVersionDto(v)).orElse(null);
        dto.currentVersion = list.getCurrentVersion().map(v-> versionMapper.toListVersionDto(v)).orElse(null);
        return dto;
    }


    public ReglissListDto toDetailsDto(ReglissList list) {
        ReglissListDto dto = toDto(list);
        List<ListVersionVO> allVersionByAltListId
                = versionRepository.getAllVersionDtoByListId(list.getId());
        dto.versions = allVersionByAltListId.stream()
                .map(v -> versionMapper.toListVersionDto(v, list))
                .sorted(comparing(ListVersionDto::getVersionNumber).reversed())
                .collect(toList());
        return dto;
    }

    public BroadcastDto toBroadcastDto(ReglissList list) {
        BroadcastDto dto = new BroadcastDto();
        dto.commentEn = list.getCommentEn();
        dto.commentFr = list.getCommentFr();
        dto.emails = list.getBroadcastEmails().stream()
                .map(this::toBroadcastEmailDto)
                .sorted(comparing(d -> d.getEmail().toUpperCase()))
                .collect(toList());

        return dto;
    }

    private ReglissListDto toDto(ReglissList list) {
        ReglissListDto dto = new ReglissListDto();
        dto.id = list.getId();
        dto.shortName = list.getShortName();
        dto.name = list.getName();
        dto.regulatorOrInternalPolicy = list.getRegulatorOrInternalPolicy().getId();
        dto.description = list.getDescription();
        dto.confidentiality = list.getConfidentiality();
        dto.country = list.getCountry().getId();
        dto.listCategory = list.getListCategory().getId();
        dto.issuer = list.getIssuer().getId();
        dto.owner = list.getOwner();
        dto.reference = list.getReference();
        dto.type = list.getType();
        dto.scope = list.getScope();
        dto.active = list.isActive();
        dto.isGlobalPlusOfac = list.isGlobalPlusOfac();
        dto.commentEn = list.getCommentEn();
        dto.commentFr = list.getCommentFr();
        dto.broadcastEmails = list.getBroadcastEmails().stream()
                .map(this::toBroadcastEmailDto)
                .sorted(comparing(d -> d.getEmail().toUpperCase()))
                .collect(toList());
        dto.territory = list.getCountry().getId();

        dto.canDeactivate = listRepository.canDeativate(list.getId()) > 0;
        dto.canDelete = listRepository.canDelete(list.getId()) > 0;
        dto.breadcrumbDto = new BreadcrumbDto();
        dto.breadcrumbDto.listName = dto.shortName;
        dto.breadcrumbDto.listReglissReference = dto.reference;
        dto.genericFormat = list.isGenericFormat();
        dto.fircosoftFormat = list.isFircosoftFormat();
        dto.reglissFormat = list.isReglissFormat();
        dto.safewatchFormat = list.isSafewatchFormat();
        dto.vigilanceFormat = list.isVigilanceFormat();
        dto.djCoreXmlFormat = list.isDjCoreXmlFormat();
        dto.geographicData = (list.getGeographicData() != null) ? list.getGeographicData().getId() : null;
        dto.filters = list.getFilters()
                .stream()
                .sorted(Comparator.comparing(DJExportFilter::getId))
                .map(watchlistFormat -> djExportFilterMapper.toDJExportFilterAndSubfiltersDto(watchlistFormat))
                .collect(toList());
        dto.sixFilters = list.getSixFilters()
                .stream()
                .sorted(Comparator.comparing(SixFilters::getId))
                .map(sixFilter -> sixFiltersMapper.toSixFilterAndSubfiltersDto(sixFilter))
                .collect(toList());
        dto.isReadOnly = listPermissionService.isListReadOnly();
        dto.isVersionReadOnly = listPermissionService.isVersionReadOnly(new ListPermissionRequest(list));
        dto.isLastVersionPublishedOrDelivered = list.getLastVersion().map(v -> v.getStatus() == VersionStatus.PUBLISHED_OK ||
                        v.getStatus() == VersionStatus.PUBLISHED_KO ||  v.getStatus() == VersionStatus.DELIVERED_OK ||
                        v.getStatus() == VersionStatus.DELIVERED_KO)
                .orElse(false);
        mapMappingFile(list, dto);
        mapImportConfiguration(list, dto);
        return dto;
    }

    private void mapMappingFile(ReglissList list, ReglissListDto dto) {
        Optional<MappingFile> mappingFileOpt;
        if (list.allowsManualUpload()) {
            mappingFileOpt = list.getActiveMappingFile();
        } else {
            mappingFileOpt = list.getMappingFiles().stream()
                    .max(Comparator.comparingLong(MappingFile::getId));
        }
        dto.mappingFile = mappingFileOpt.map(mappingFileMapper::toDto).orElse(null);
    }

    private void mapImportConfiguration(ReglissList list, ReglissListDto dto) {
        if (list.getImportConfiguration() != null) {
            ImportConfiguration importConfiguration = list.getImportConfiguration();
            dto.importConfiguration = new ImportConfigurationDto();
            dto.importConfiguration.checksumReception = importConfiguration.isChecksumReception();
            dto.importConfiguration.allowIncrementalImport = importConfiguration.isAllowIncrementalImport();
            dto.importConfiguration.fileName = importConfiguration.getFileNamePrefix();
            dto.importConfiguration.importFileType = importConfiguration.getImportFileType();
            dto.importConfiguration.logReception = importConfiguration.isLogReception();
            dto.importConfiguration.confirmationRequired = importConfiguration.isConfirmationRequired();
            dto.importConfiguration.autoConfirmation = importConfiguration.getAutoConfirmationThreshold() != null;
            dto.importConfiguration.autoConfirmationThreshold = importConfiguration.getAutoConfirmationThreshold();
            dto.importConfiguration.allowAutoUpload = importConfiguration.isAllowAutoUpload();
            dto.importConfiguration.allowManualUpload = importConfiguration.isAllowManualUpload();
        }
    }

    public ReglissList toEntity(ReglissList entity, ReglissListDto dto) {
        entity.setRegulatorOrInternalPolicy(reference.getRegulatorOrInternalPolicyById(dto.regulatorOrInternalPolicy));
        entity.setDescription(dto.description);
        entity.setName(dto.name);
        entity.setShortName(dto.shortName);
        entity.setConfidentiality(dto.confidentiality);
        entity.setCountry(reference.getCountryById(dto.country));
        entity.setListCategory(reference.getCategoryById(dto.listCategory));
        entity.setActive(dto.active);
        entity.setGlobalPlusOfac(dto.isGlobalPlusOfac);
        entity.setDeleted(false);
        entity.setIssuer(reference.getIssuerById(dto.issuer));
        entity.setOwner(dto.owner);
        entity.setReference(dto.reference);
        entity.setScope(dto.scope);
        entity.setType(dto.type);
        entity.setCommentEn(dto.commentEn);
        entity.setCommentFr(dto.commentFr);
        entity.setGenericFormat(dto.genericFormat);
        entity.setFircosoftFormat(dto.fircosoftFormat);
        entity.setReglissFormat(dto.reglissFormat);
        entity.setSafewatchFormat(dto.safewatchFormat);
        entity.setVigilanceFormat(dto.vigilanceFormat);
        entity.setGeographicData(dto.geographicData != null ? reference.getGeographicDataById(dto.geographicData) : null);
        return entity;
    }

    public ImportConfiguration toImportConfigurationEntity(ImportConfigurationDto importConfigurationDto) {
        return new ImportConfiguration()
                .setChecksumReception(importConfigurationDto.checksumReception)
                .setAllowIncrementalImport(importConfigurationDto.allowIncrementalImport)
                .setLogReception(importConfigurationDto.logReception)
                .setImportFileType(importConfigurationDto.importFileType)
                .setFileNamePrefix(importConfigurationDto.fileName)
                .setConfirmationRequired(importConfigurationDto.confirmationRequired)
                .setAutoConfirmationThreshold(importConfigurationDto.autoConfirmationThreshold)
                .setAllowAutoUpload(importConfigurationDto.allowAutoUpload)
                .setAllowManualUpload(importConfigurationDto.allowManualUpload);
    }

    public void mapBroadcastEmails(ReglissList entity, List<BroadcastEmailDto> broadcastEmails) {
        Set<BroadcastEmail> newEmails = broadcastEmails.stream()
                .map(dtoEmail -> new BroadcastEmail(dtoEmail.id, dtoEmail.email)).collect(Collectors.toSet());

        EntityCollectionDiff<BroadcastEmail> emailDiff = new EntityCollectionDiff<>(entity.getBroadcastEmails(),
                newEmails, e -> Arrays.asList(e.getId()));

        for (BroadcastEmail toCreate : emailDiff.getCreatedElements()) {
            entity.addBroadcastEmail(new BroadcastEmail(toCreate.getEmail()));
        }

        for (Map.Entry<BroadcastEmail, BroadcastEmail> toModify : emailDiff.getUpdatedElements().entrySet()) {
            toModify.getKey().setEmail(toModify.getValue().getEmail());
        }

        for (BroadcastEmail toDelete : emailDiff.getDeletedElements()) {
            entity.removeBroadcastEmail(toDelete);
        }
    }

    private BroadcastEmailDto toBroadcastEmailDto(BroadcastEmail entity) {
        BroadcastEmailDto dto = new BroadcastEmailDto();
        dto.id = entity.getId();
        dto.email = entity.getEmail();
        return dto;
    }

    public ListBreadcrumbDto toListBreadcrumbDto(ListBreadcrumbVO list) {
        ListBreadcrumbDto dto = new ListBreadcrumbDto();
        dto.name = list.getName();
        dto.shortName = list.getShortName();
        dto.reference = list.getReference();
        dto.importFileType = list.getImportFileType();
        dto.canCreateVersion = (list.getImportFileType() == ImportFileType.MANUAL_PROCESSING
                || list.getImportFileType() == ImportFileType.NOT_SUPPORTED
                || list.getImportFileType() == ImportFileType.CRB_LIST
                || (list.getImportFileType() == ImportFileType.DOW_JONES_CORE && list.isAllowManualUpload())
                || (list.getImportFileType() == ImportFileType.CUSTOM && list.isAllowManualUpload()))
                        && list.isActive()
                        && !(listPermissionService.isVersionReadOnly(new ListPermissionRequest(list)));
        return dto;
    }
}
