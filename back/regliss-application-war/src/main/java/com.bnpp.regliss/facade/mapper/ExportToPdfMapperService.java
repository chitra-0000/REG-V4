package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.facade.dto.pdf.*;
import com.bnpp.regliss.facade.dto.record.*;
import com.bnpp.regliss.repository.RecordAssociationRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.PdfReferenceService;
import com.bnpp.regliss.service.ReferenceService;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.eclipse.jdt.internal.compiler.util.Util.EMPTY_STRING;

@Service
public class ExportToPdfMapperService {

    /**
     *   Pdf report mapping used inside the jrxml export
     */

    @Autowired
    private ReferenceService referenceService;
    @Autowired
    private PdfReferenceService pdfReferenceService;

    @Autowired
    private RecordAssociationRepository associationRepo;
    @Autowired
    private VersionRepository versionRepo;
    @Autowired
    private RecordRepository recordRepo;

    private static final EnumMap<RecordType, String> RECORD_TYPE_VALUES = new EnumMap<>(RecordType.class);
    private static final EnumMap<ImportFileType, String> IMPORT_FILE_TYPE_VALUES = new EnumMap<>(ImportFileType.class);

    static {
        RECORD_TYPE_VALUES.put(RecordType.AIRPORT, "Airport");
        RECORD_TYPE_VALUES.put(RecordType.CITY, "City");
        RECORD_TYPE_VALUES.put(RecordType.COUNTRY, "Country");
        RECORD_TYPE_VALUES.put(RecordType.ENTITY, "Entity");
        RECORD_TYPE_VALUES.put(RecordType.PERSON, "Individual");
        RECORD_TYPE_VALUES.put(RecordType.OTHER, "Other");
        RECORD_TYPE_VALUES.put(RecordType.SEAPORT, "Seaport");
        RECORD_TYPE_VALUES.put(RecordType.VESSEL, "Vessel");

        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.DOW_JONES_CORE, "DowJonesCore");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.DOW_JONES_WATCHLIST, "DowJonesWatchlist");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.DOW_JONES_AME, "DowJones");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.CUSTOM, "Custom");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.CRB_LIST, "CRBList");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.MANUAL_PROCESSING, "ManualProcessing");
        IMPORT_FILE_TYPE_VALUES.put(ImportFileType.NOT_SUPPORTED, "NotSupported");
    }

    public String getCategoriesLabelsById(List<Long> categoryIds) {
        return categoryIds.stream().map(o -> getReferential(o, referenceService::getRecordCategory)).collect(Collectors.joining(", "));
    }

    public String getSanctionTypesLabelById(List<Long> sanctionTypeIds) {
        return sanctionTypeIds.stream().map(o -> getReferential(o, referenceService::getSanctionTypeById))
                .collect(Collectors.joining(", "));
    }

    public String getGendersLabelById(List<Long> gendersIds) {
        return gendersIds.stream().map(o -> getReferential(o, pdfReferenceService::getGenderById)).collect(Collectors.joining(", "));
    }

    private String getReferential(Long typeId, Function<Long, RecordReferential> mapper) {
        return Optional.ofNullable(typeId).map(mapper).map(RecordReferential::getLabel).orElse(EMPTY_STRING);
    }

    public String getTypeLabelByType(RecordType type) { return RECORD_TYPE_VALUES.getOrDefault(type, type.name()); }

    public String getImportFileLabelByType(ImportFileType importFileType) {
        return IMPORT_FILE_TYPE_VALUES.getOrDefault(importFileType, importFileType.name());
    }

    public List<PdfDateDetailsDto> getBiographyDetails(List<PersonBiographyDto> dtos) {
        return dtos.stream().map(o -> new PdfDateDetailsDto(o, getReferential(o.dateType, referenceService::getDateTypeById),
                getReferential(o.country, referenceService::getCountryById))).collect(Collectors.toList());
    }

    public List<PdfDateDetailsDto> getDatesDetails(List<DateDetailsDto> dtos) {
        return dtos.stream().map(o -> new PdfDateDetailsDto(o, getReferential(o.dateType, referenceService::getDateTypeById)))
                .collect(Collectors.toList());
    }

    public List<PdfAddressDto> getAddresses(List<AddressDto> dtos) {
        return dtos.stream().map(o -> new PdfAddressDto(o, getReferential(o.country, referenceService::getCountryById)))
                .collect(Collectors.toList());
    }

    public List<PdfIdentityDocumentDto> getIdentityDocuments(List<IdentityDocumentDto> dtos) {
        return dtos.stream().map(o -> new PdfIdentityDocumentDto(o, getReferential(o.type, referenceService::getIdentityDocumentTypeById),
                getReferential(o.country, referenceService::getCountryById))).collect(Collectors.toList());
    }

    public List<PdfCountryDetailsDto> getCountryDetails(List<CountryDetailsDto> dtos) {
        return dtos.stream().map(o -> new PdfCountryDetailsDto(o, getReferential(o.type, referenceService::getCountryTypeById),
                getReferential(o.country, referenceService::getCountryById))).collect(Collectors.toList());
    }

    public List<PdfAliasDto> getAliasesDetailes(List<AliasDto> dtos) {
        return dtos.stream().map(o -> new PdfAliasDto(o, getReferential(o.type, referenceService::getAliasTypeById),
                getReferential(o.quality, referenceService::getAliasQualityById))).collect(Collectors.toList());
    }

    public List<PdfProgramDto> getProgramsDetailes(List<ProgramDto> dtos) {
        return dtos.stream().map(o -> new PdfProgramDto(o, getReferential(o.sanctionType, referenceService::getProgramTypeById)))
                .collect(Collectors.toList());
    }

    public List<PdfRecordRelationDto> getRecordRelationsDetailes(List<RecordRelationDto> dtos) {
        return dtos.stream().map(o -> new PdfRecordRelationDto(o, getReferential(o.category, referenceService::getRecordCategory),
                o.type!=null ? getTypeLabelByType(o.type):""
                , getReferential(o.relationTypeId, referenceService::getRelationTypeById))).collect(Collectors.toList());
    }

    public List<PdfPersonFunctionDto> getFunctionDetailes(List<PersonFunctionDto> dtos) {
        return dtos.stream().map(o -> new PdfPersonFunctionDto(o, getReferential(o.pepOccupationCategory,
                pdfReferenceService::getPepOccupationCategoryById))).collect(Collectors.toList());
    }

    public String getSearchCode(RecordDto recordDto) {
        SearchContainer searchContainer = ImmutableMap.builder()
                .put(RecordType.AIRPORT, () -> recordDto.airportFields.searchCode)
                .put(RecordType.CITY, () -> recordDto.cityFields.searchCode)
                .put(RecordType.COUNTRY, () -> recordDto.countryFields.searchCode)
                .put(RecordType.OTHER, () -> recordDto.otherFields.searchCode)
                .put(RecordType.SEAPORT, () -> recordDto.seaportFields.searchCode)
                .build().getOrDefault(recordDto.type, () -> EMPTY_STRING);
        return searchContainer != null ? searchContainer.getSearchCode() : null;
    }

    interface SearchContainer {
        String getSearchCode();
    }

    public List<RecordRelationDto> ifWatchlistAceptanceList(String recordID, Long versionId) {
        Record record = recordRepo.findByUUIDAndVersionId(recordID, versionId)
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND, recordID));
        Version version = versionRepo.findById(versionId)
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
        return toAceptanceList(version, record);
    }

    public List<RecordRelationDto> toAceptanceList(Version version, Record record) {
        if(version.getImportFileType().isWatchlistFormat()) {
            return associationRepo.getWatchlistAssociationsFieldsByIdAndList(
                    record.getId(), version.getList().getId(), version.getId())
                    .stream()
                    .flatMap(x -> mapToRelationDto(x).stream())
                    .collect(toList());
        }
        return Collections.emptyList();
