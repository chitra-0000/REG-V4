package com.bnpp.regliss.facade;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.config.facade.Facade;
import com.bnpp.regliss.entity.ListVersionDTO;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.record.RecordVersionLinks;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.external.facade.dto.record.RecordVigilanceDto;
import com.bnpp.regliss.external.facade.dto.record.VigilanceRecordSearchCriteriaDto;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;
import com.bnpp.regliss.facade.dto.record.ElementDetailExportDto;
import com.bnpp.regliss.facade.dto.record.RecordDescriptionDto;
import com.bnpp.regliss.facade.dto.record.RecordDto;
import com.bnpp.regliss.facade.mapper.ExportToPdfMapperService;
import com.bnpp.regliss.facade.mapper.RecordDescriptionMapper;
import com.bnpp.regliss.facade.mapper.RecordMapper;
import com.bnpp.regliss.facade.mapper.external.RecordVigilanceMapper;
import com.bnpp.regliss.facade.mapper.external.RecordVigilanceSearchMapper;
import com.bnpp.regliss.facade.mapper.external.VigilanceSearchCriteriaMapper;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.search.VigilanceRecordSearchCriteria;
import com.bnpp.regliss.service.pdf.JasperExportPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Facade
@ReglissExternalAPIProfile
public class RecordExternalFacade {

    @Autowired
    private RecordRepository recordRepo;
    @Autowired
    private VigilanceRecordSearchRepository recordSearchRepo;
    @Autowired
    private VersionRepository versionRepo;
    @Autowired
    private ReglissListRepository reglissListRepo;
    @Autowired
    private RecordMapper mapper;
    @Autowired
    private RecordVigilanceMapper mapperVigilance;
    @Autowired
    private RecordVigilanceSearchMapper mapperSearchVigilance;
    @Autowired
    private VigilanceSearchCriteriaMapper vigilanceSearchCriteriaMapper;
    @Autowired
    private JasperExportPdfService jasperExportPdfService;
    @Autowired
    private ExportToPdfMapperService mapperService;
    @Autowired
    private RecordCategoryRepository recordCategoryRepo;
    @Autowired
    private RecordDescriptionMapper recordDescriptionMapper;

    public ByteArrayOutputStream exportPdf(String uuid) {

        RecordDto record = getRecord(uuid);
        ElementDetailExportDto detailDto = new ElementDetailExportDto(record, mapperService);

        return jasperExportPdfService.exportPdfFile(detailDto);
    }

    public RecordDto getRecord(String uuid) {
        Record record = recordRepo.findByExternalUuid(uuid)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND,uuid));

        RecordDto recordDto = mapper.toDto(record);
        recordDto.generalFields.readOnly = true;
        recordDto.hasWritePermissionOnList = false;
        recordDto.jasperPdfDownload = jasperPdfDownload(record.getVersionLinks());
        recordDto.generalFields.breadcrumbDto = getBreadrumDtoNoVersion(record);
        return recordDto;
    }

    public List<RecordDescriptionDto> getAllDescriptions(){
        List<RecordCategory> recordCategories = recordCategoryRepo.findAll();

        List<RecordDescriptionDto> descriptionsDtos = recordCategories.stream()
                .map(recordCategory -> recordDescriptionMapper.recordCategoryToDto(recordCategory))
                .collect(Collectors.toList());

        return descriptionsDtos;
    }

    private BreadcrumbDto getBreadrumDtoNoVersion(Record record) {
        ReglissList list = reglissListRepo.getExactlyOne(record.getVersionLinks().getListId());
        BreadcrumbDto breadcrumbDto = new BreadcrumbDto();
        breadcrumbDto.listName = list.getName();
        breadcrumbDto.listReglissReference = list.getReference();
        breadcrumbDto.idList = list.getId();
        breadcrumbDto.importFileType = list.getImportFileType();
        return breadcrumbDto;
    }

    public RecordVigilanceDto getVigilanceRecordWithProgramsByRecordId(String recordID, Long versionNo, String listReference) {
        Record record = recordRepo.findByRecordIdAndVersionNoAndListReference(recordID, versionNo, listReference)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND, recordID, versionNo.toString(), listReference));
        RecordVigilanceDto dto = mapperVigilance.toVigilanceDto(record);
        Optional<Version> versionOpt = versionRepo.getByVersionNumberAndListReference(versionNo, listReference);
        versionOpt.ifPresent(version -> {
            dto.generalFields.list = mapperVigilance.toVigilanceListWithPrograms(version);
            dto.generalFields.relations.addAll(mapperVigilance.toAceptanceList(version, record));
        });

        return dto;
    }

    public RecordVigilanceDto getVigilanceRecordByRecordExtRef(String recordExtRef, Long versionNo, String listReference) {
        Record record = recordRepo.findByRecordExternalRefferenceAndVersionNoAndListReference(recordExtRef, versionNo, listReference)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND, recordExtRef, versionNo.toString(), listReference));

        return toRecordVigilanceDto(versionNo, listReference, record);
    }

    public RecordVigilanceDto getVigilanceRecordByRecordExtRefV2(String recordExtRef, Long versionNo, String listReference) {
        Record record = recordRepo.findByRecordExternalRefferenceAndVersionNoAndListReference(recordExtRef, versionNo, listReference)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND, recordExtRef, versionNo.toString(), listReference));

        return toRecordVigilanceDtoV2(versionNo, listReference, record);
    }

    public RecordVigilanceDto getVigilanceRecordByRecordIdV2(String recordID, Long versionNo, String listReference) {
        Record record = recordRepo.findByRecordIdAndVersionNoAndListReference(recordID, versionNo, listReference)
                .orElseThrow(() -> new ReglissException(ErrorCode.NOT_FOUND, recordID, versionNo.toString(), listReference));
        return toRecordVigilanceDtoV2(versionNo, listReference, record);
    }

    private RecordVigilanceDto toRecordVigilanceDto(Long versionNo, String listReference, Record record) {
        RecordVigilanceDto dto = mapperVigilance.toVigilanceDto(record);
        Optional<Version> versionOpt = versionRepo.getByVersionNumberAndListReference(versionNo, listReference);
        versionOpt.ifPresent(version -> {
            dto.generalFields.list = mapperVigilance.toListDto(version);
            dto.generalFields.relations.addAll(mapperVigilance.toAceptanceList(version, record));
        });
        return dto;
    }

    private RecordVigilanceDto toRecordVigilanceDtoV2(Long versionNo, String listReference, Record record) {
        RecordVigilanceDto dto = mapperVigilance.mapCommonFields(record);
        Optional<ListVersionDTO> versionOpt = versionRepo.getVLDataByVersionNumberAndListReference(versionNo, listReference);
        versionOpt.ifPresent(version -> {
            dto.generalFields.list = mapperVigilance.toListDtoV2(version);
            dto.generalFields.relations.addAll(mapperVigilance.toAceptanceListV2(version, record));
        });
        return dto;
    }

    public List<RecordVigilanceDto> searchRecord(VigilanceRecordSearchCriteriaDto searchCriteriaDto) {
        VigilanceRecordSearchCriteria searchCriteria = vigilanceSearchCriteriaMapper.mapVigilanceRecordSearchCriteriaFromDto(searchCriteriaDto);
        if (searchCriteria.searchedVersions.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Record> queryResult = recordSearchRepo.query(searchCriteria);

        Map<Long, ListVersionDTO> searchedVersions = searchCriteria.searchedVersions
                .stream().collect(Collectors.toMap(ListVersionDTO::getListId, identity()));
        return queryResult.stream()
                .map(record -> mapperSearchVigilance.toVigilanceSearchResultDto(record, searchedVersions))
                .collect(Collectors.toList());
    }

    private boolean jasperPdfDownload(RecordVersionLinks recordVersionLinks){
        ReglissList reglissList = reglissListRepo.getExactlyOne(recordVersionLinks.getListId());

        switch(reglissList.getConfidentiality()){
            case PUBLIC:
            case RESTRICTED:
                return true;
            case CONFIDENTIAL:
                return false;
            default:
                break;
        }
        return false;
    }
    public HttpHeaders configureHttpContent(String genericReportName) {
        return jasperExportPdfService.configureHttpContent(genericReportName);
    }
}

