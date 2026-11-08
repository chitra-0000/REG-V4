package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.User;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.entity.referential.record.*;
import com.bnpp.regliss.facade.dto.BreadcrumbDto;
import com.bnpp.regliss.facade.dto.record.*;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.AddModSupRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.service.VersionLockService;
import com.bnpp.regliss.utils.FormatDateUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class RecordMapper {

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private AddModSupRepository addModSupRepo;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    private VersionLockService versionLockService;

    public RecordDto toDto(Record record, Version version) {
        RecordCommonDto commonDto = toCommonRecordDto(record);
        setListProperties(commonDto,version);
        fillVersionInfo(version, commonDto);

        return toTypeRelatedRecorDto(record, commonDto);
    }
    public RecordDto toDto(Record record) {
        RecordCommonDto commonDto = toCommonRecordDto(record);
        return toTypeRelatedRecorDto(record, commonDto);
    }
    private RecordDto toTypeRelatedRecorDto(Record record, RecordCommonDto commonDto) {
        switch (record.getType()) {
        case PERSON:
            RecordDto recordDto = new RecordDto(record.getExternalUuid(), commonDto, toPersonDto((PersonRecord) record));
            mapPersonName(recordDto);
            return recordDto;
        case ENTITY:
            return new RecordDto(record.getExternalUuid(), commonDto, toEntityRecordDto((EntityRecord) record));
        case VESSEL:
            return new RecordDto(record.getExternalUuid(), commonDto, toVesselRecordDto((VesselRecord) record));
        case CITY:
            return new RecordDto(record.getExternalUuid(), commonDto, toCityRecordDto((CityRecord) record));
        case COUNTRY:
            return new RecordDto(record.getExternalUuid(), commonDto, toCountryRecordDto((CountryRecord) record));
        case AIRPORT:
            return new RecordDto(record.getExternalUuid(), commonDto, toAirportRecordDto((AirportRecord) record));
        case SEAPORT:
            return new RecordDto(record.getExternalUuid(), commonDto, toSeaportRecordDto((SeaportRecord) record));
        case OTHER:
            return new RecordDto(record.getExternalUuid(), commonDto, toOtherRecordDto((OtherRecord) record));
        default:
            throw new ReglissException("Invalid record type: " + record.getType());
        }
    }

    private void mapPersonName(RecordDto recordDto) {
        recordDto.generalFields.name= Stream.of(recordDto.personFields.title,
                                                recordDto.personFields.firstName,
                                                recordDto.personFields.middleName,
                                                StringUtils.upperCase(recordDto.personFields.lastName),
                                                recordDto.personFields.suffix)
                                    .filter(StringUtils::isNotBlank)
                                    .collect(Collectors.joining(" "));
    }


    private PersonRecordDto toPersonDto(PersonRecord record) {
        PersonRecordDto personDto = new PersonRecordDto();
        personDto.genders = record.getGenders().stream()
                .map(Gender:: getId)
                .collect(toList());
        personDto.title = record.getTitle();
        personDto.firstName = record.getFirstName();
        personDto.middleName = record.getMiddleName();
        personDto.lastName = record.getLastName();
        personDto.maidenName = record.getMaidenName();
        personDto.suffix = record.getSuffix();
        personDto.isDeceased = record.getDeceased();
        personDto.iban = record.getIbanCodes().stream().map(this::toIbanDto).collect(toSet());
        personDto.personImages = record.getPersonImages().stream().map(this::toPersonImageDto).collect(toSet());
        personDto.addresses = record.getAddresses().stream().map(this::toAddressDto).collect(toList());
        personDto.identityDocuments = record.getIdentityDocuments().stream().map(this::toIdentityDocumentDto).collect(toList());
        personDto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto).collect(toList());
        personDto.personFunctions = record.getPersonFunctions().stream().map(this::toFunctionRecord).collect(toList());
        personDto.biography = record.getBiographies().stream().map(this::toBiographyDto).collect(toList());
        return personDto;
    }

    private EntityRecordDto toEntityRecordDto(EntityRecord record){
        EntityRecordDto dto = new EntityRecordDto();
        dto.iban = record.getIbanCodes().stream().map(this::toIbanDto).collect(toSet());
        dto.bic = record.getBicCodes().stream().map(this::toBicDto).collect(toSet());
        dto.isin = record.getIsinCodes().stream().map(this::toIsinDto).collect(toSet());
        dto.cusip = record.getCusipCodes().stream().map(this::toCusipDto).collect(toSet());
        dto.addresses = record.getAddresses().stream().map(this::toAddressDto).collect(toList());
        dto.datesDetails = record.getDatesDetails().stream().map(this::toDateDetailsDto).collect(toList());
        dto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto).collect(toList());
        dto.identityDocuments = record.getIdentityDocuments().stream().map(this::toIdentityDocumentDto).collect(toList());
        return dto;
    }

    private CodeIbanDto toIbanDto (CodeIban code) {
        CodeIbanDto dto = new CodeIbanDto();
        dto.id = code.getId();
        dto.code = code.getCode();
        return dto;
    }

    private PersonImageDto toPersonImageDto (PersonImage personImage) {
        PersonImageDto dto = new PersonImageDto();
        dto.id = personImage.getId();
        dto.url = personImage.getUrl();
        return dto;
    }
    private CodeBicDto toBicDto (CodeBic record) {
        CodeBicDto dto = new CodeBicDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }
    private CodeIsinDto toIsinDto (CodeIsin record) {
        CodeIsinDto dto = new CodeIsinDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }
    private CodeCusipDto toCusipDto (CodeCusip record) {
        CodeCusipDto dto = new CodeCusipDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }

    private VesselRecordDto toVesselRecordDto(VesselRecord record){
        VesselRecordDto dto = new VesselRecordDto();
        dto.imo = record.getImo();
        dto.flag = record.getFlag();
        dto.owner = record.getOwner();
        dto.vesselType = record.getVesselType();
        dto.tonnage = record.getTonnage();
        dto.grossTonnage = record.getGrossTonnage();
        dto.callSign = record.getCallSign();
        dto.datesDetails = record.getDatesDetails().stream().map(this::toDateDetailsDto).collect(toList());
        dto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto).collect(toList());
        dto.identityDocuments = record.getIdentityDocuments().stream().map(this::toIdentityDocumentDto).collect(toList());
        dto.addresses = record.getAddresses().stream().map(this::toAddressDto).collect(toList());
        return dto;
    }

    private CityRecordDto toCityRecordDto(CityRecord record){
        CityRecordDto dto = new CityRecordDto();
        dto.searchCode = record.getSearchCode();
        return dto;
    }

    private AirportRecordDto toAirportRecordDto(AirportRecord record){
        AirportRecordDto dto = new AirportRecordDto();
        dto.searchCode = record.getSearchCode();
        return dto;
    }

    private SeaportRecordDto toSeaportRecordDto(SeaportRecord record){
        SeaportRecordDto dto = new SeaportRecordDto();
        dto.searchCode = record.getSearchCode();
        return dto;
    }

    private CountryRecordDto toCountryRecordDto(CountryRecord record){
        CountryRecordDto dto = new CountryRecordDto();
        dto.searchCode = record.getSearchCode();
        dto.isoNum = record.getIsoNum();
        dto.isoAlpha = record.getIsoAlpha();
        return dto;
    }

    private OtherRecordDto toOtherRecordDto(OtherRecord record){
        OtherRecordDto dto = new OtherRecordDto();
        dto.category = record.getOtherCategory();
        dto.searchCode = record.getSearchCode();
        return dto;
    }

    private RecordCommonDto toCommonRecordDto(Record record) {
        RecordCommonDto commonDto = new RecordCommonDto();
        commonDto.name = record.getName();
        commonDto.description =  record.getDescription();
        commonDto.keywords= record.getKeywords();
        commonDto.notes = record.getDowJonesNotes();
        commonDto.reglissReference = record.getReglissReference();
        commonDto.externalReference = record.getExternalReference();
        commonDto.deleteDate =  getDeletionDate(record).orElse(null);
        if (StringUtils.isNotEmpty(commonDto.deleteDate)) {
            commonDto.synthesisModDate = commonDto.deleteDate;
        } else {
            commonDto.synthesisModDate = StringUtils.isNotEmpty(record.getSynthesisModDate()) ?
                    FormatDateUtil.handleWrongFormatDate(record.getSynthesisModDate()):null;
        }
        commonDto.startDate = FormatDateUtil.handleWrongFormatDate(record.getStartDateStr());
        commonDto.comments = record.getComments();
        commonDto.additionalInfo = record.getAdditionalInfo();
        commonDto.instruction = record.getInstruction();
        commonDto.recordId = record.getRecordId();
        commonDto.sanctionTypes = record.getSanctionTypes().stream()
                .sorted(comparing(SanctionType::getLabel))
                .map(SanctionType:: getId)
                .collect(toList());
        commonDto.programsIds = record.getPrograms().stream().map(Program::getId).collect(toList());
        commonDto.programs = record.getPrograms().stream().map(program -> new ProgramDto(program, null)).collect(toList());
        commonDto.aliases = mapAliasesBasedOnRecordType(record);
        commonDto.sanctionReferences = record.getSanctionReferences().stream()
                .map(this::toSanctionReferenceDto).collect(toList());
        commonDto.sources = record.getSources().stream()
                .map(this::toRecordSourceDto).collect(toList());
        commonDto.relations = record.getRelations().stream().map(this::toRecordRelationDto).collect(toList());
        commonDto.categories = record.getCategories().stream().sorted(comparing(RecordReferential::getLabel))
                .map(RecordReferential::getId).collect(toList());
        commonDto.descriptions = record.getDescriptions3().stream().sorted(comparing(RecordReferential::getLabel))
                .map(RecordReferential::getId).collect(toList());
        return commonDto;
    }
    private void setListProperties(RecordCommonDto commonDto,  Version version ) {
        ReglissList reglissList = version.getList();
        commonDto.importFileType = version.getImportFileType();
        commonDto.listConfidentiality = reglissList.getConfidentiality();
    }

    private void fillVersionInfo(Version version, RecordCommonDto commonDto) {
        commonDto.idVersion = version.getId();
        commonDto.breadcrumbDto = new BreadcrumbDto(version);
        commonDto.isVersionLocked = versionLockService.isLockedByAnotherUser(version.getId());
        if(commonDto.isVersionLocked) {
            commonDto.lockingUser = versionLockService.getLockingUser(version.getId()).map(User::getUsername).orElse(null);
        }
    }

    private List<AliasDto> mapAliasesBasedOnRecordType(Record record) {
        Function<Alias, AliasDto> aliasMappingFunction;
        if (needsFullNameMapping(record)) {
            aliasMappingFunction = this::toAliasDtoOtherThanPerson;
        } else {
            aliasMappingFunction = this::toAliasDto;
        }
        return record.getAliases().stream()
            .sorted(comparing(Alias::getReglissReference))
            .map(aliasMappingFunction).collect(toList());
    }

    private boolean needsFullNameMapping(Record record) { return record.getType() != RecordType.PERSON; }

    private Optional<String> getDeletionDate(Record record) {
        Optional<LocalDate> date = recordRepo.getDeletionDate(record);
        return date.map(FormatDateUtil::getDateStringFromLocalDate);
    }

    private AliasDto toAliasDto(Alias alias){
        AliasDto aliasDto = toBaseAlias(alias);
        aliasDto.lastName = alias.getLastName();
        aliasDto.suffix = alias.getSuffix();
        return aliasDto;
    }

    private AliasDto toAliasDtoOtherThanPerson(Alias alias){
        AliasDto aliasDto = toBaseAlias(alias);
        aliasDto.lastName = alias.getFullName();
        aliasDto.suffix = alias.getSuffix();
        return aliasDto;
    }

    private AliasDto toBaseAlias(Alias alias) {
        AliasDto aliasDto = new AliasDto();
        aliasDto.id = alias.getId();
        aliasDto.externalReference = alias.getExternalReference();
        aliasDto.reglissReference = alias.getReglissReference();
        aliasDto.title = alias.getTitle();
        aliasDto.firstName = alias.getFirstName();
        aliasDto.middleName = alias.getMiddleName();
        aliasDto.type = alias.getTypeOpt().map(AbstractSimpleEntity::getId).orElse(null);
        aliasDto.quality = alias.getQualityOpt().map(AbstractSimpleEntity::getId).orElse(null);
        aliasDto.language = alias.getLanguage();
        return aliasDto;
    }

    private SanctionReferenceDto toSanctionReferenceDto(SanctionReference sanctionReference){
        SanctionReferenceDto sanctionReferenceDto = new SanctionReferenceDto();
        sanctionReferenceDto.code = sanctionReference.getCode();
        sanctionReferenceDto.listName = sanctionReference.getListName();
        sanctionReferenceDto.id = sanctionReference.getId();
        sanctionReferenceDto.remarks = sanctionReference.getRemarks();
        sanctionReferenceDto.name = sanctionReference.getListName();
        sanctionReferenceDto.sinceDate = FormatDateUtil.handleWrongFormatDate(sanctionReference.getSinceDateStr());
        sanctionReferenceDto.endDate = FormatDateUtil.handleWrongFormatDate(sanctionReference.getEndDateStr());
        return sanctionReferenceDto;
    }

    private RecordRelationDto toRecordRelationDto(RecordRelation recordRelation){
        RecordRelationDto recordRelationDto = new RecordRelationDto();
        recordRelationDto.id = recordRelation.getId();
        recordRelationDto.category = recordRelation.getCategoryOpt().map(RecordCategory::getId).orElse(null);
        recordRelationDto.name = recordRelation.getName();
        recordRelationDto.type = recordRelation.getRecordType();
        recordRelationDto.ex = recordRelation.getEx();
        recordRelationDto.relationTypeId = recordRelation.getRelationTypeOpt().map(AbstractSimpleEntity::getId).orElse(null);
        return recordRelationDto;
    }


    private RecordSourceDto toRecordSourceDto(RecordSource source){
        RecordSourceDto recordSourceDto = new RecordSourceDto();
        recordSourceDto.id = source.getId();
        recordSourceDto.name = source.getName();
        return recordSourceDto;
    }

    private CountryDetailsDto toCountryDetailsDto(CountryDetails countryDetails){
        CountryDetailsDto countryDetailsDto = new CountryDetailsDto();
        countryDetailsDto.id = countryDetails.getId();
        countryDetailsDto.additionalInfo = countryDetails.getAdditionalInfoOpt().orElse(null);
        countryDetailsDto.country = countryDetails.getCountryOpt().map(AbstractSimpleEntity::getId).orElse(null);
        countryDetailsDto.principal = countryDetails.getPrimary();
        countryDetailsDto.type = countryDetails.getTypeOpt().map(AbstractSimpleEntity::getId).orElse(null);
        return countryDetailsDto;
    }

    private PersonFunctionDto toFunctionRecord(PersonFunction entity){
        PersonFunctionDto dto = new PersonFunctionDto();
        dto.id = entity.getId();
        dto.from = FormatDateUtil.handleWrongFormatDate(entity.getFromStr());
        dto.to=FormatDateUtil.handleWrongFormatDate(entity.getToStr());
        dto.functionName=entity.getFunctionName();
        dto.pepOccupationCategory=entity.getPepOccupationCategory() != null ? entity.getPepOccupationCategory().getId() : null;
        return dto;
    }

    private PersonBiographyDto toBiographyDto(PersonBiography biography){
        PersonBiographyDto biographyDto = new PersonBiographyDto();
        biographyDto.id = biography.getId();
        biographyDto.dateValue = FormatDateUtil.handleWrongFormatDate(biography.getDateValueStrOpt().orElse(null));
        biographyDto.dateType = biography.getDateTypeOpt().map(AbstractSimpleEntity::getId).orElse(null);
        biographyDto.birthPlace = biography.getBirthPlaceOpt().orElse(null);
        biographyDto.country = biography.getBirthCountryOpt().map(AbstractSimpleEntity::getId).orElse(null);
        biographyDto.additionalInfo = biography.getAdditionalInfo();
        biographyDto.principal = biography.isPrimary();
        return biographyDto;
    }

    private DateDetailsDto toDateDetailsDto(DateDetails dateDetails){
        DateDetailsDto dateDetailsDto = new DateDetailsDto();
        dateDetailsDto.id = dateDetails.getId();
        dateDetailsDto.dateValue = FormatDateUtil.handleWrongFormatDate(dateDetails.getDateValueStr());
        dateDetailsDto.dateType = dateDetails.getDateTypeOpt().map(DateType::getId).orElse(null);
        return dateDetailsDto;
    }

    private AddressDto toAddressDto(Address address){
        AddressDto addressDto = new AddressDto();
        addressDto.id = address.getId();
        addressDto.address = address.getAddress();
        addressDto.zipCode = address.getZipCode();
        addressDto.city = address.getCity();
        addressDto.state = address.getState();
        addressDto.country = address.getCountryOpt().map(AbstractSimpleEntity::getId).orElse(null);
        addressDto.additionalAddress = address.getAdditionalAddress();
        addressDto.remarks = address.getRemarks();
        addressDto.reglissReference = address.getReglissReference();
        return addressDto;
    }

    private IdentityDocumentDto toIdentityDocumentDto(IdentityDocument identityDocument){
        IdentityDocumentDto identityDocumentDto = new IdentityDocumentDto();
        identityDocumentDto.id = identityDocument.getId();
        identityDocumentDto.additionalInfo = identityDocument.getAdditionalInfo();
        identityDocumentDto.issuingDate = FormatDateUtil.handleWrongFormatDate(identityDocument.getIssuingDateStr());
        identityDocumentDto.expirationDate = FormatDateUtil.handleWrongFormatDate(identityDocument.getExpirationDateStr());
        identityDocumentDto.country = identityDocument.getCountryOpt().map(Country::getId).orElse(null);
        identityDocumentDto.number = identityDocument.getNumberOpt().orElse(null);
        identityDocumentDto.type = identityDocument.getTypeOpt().map(AbstractSimpleEntity::getId).orElse(null);

        return identityDocumentDto;
    }

}

