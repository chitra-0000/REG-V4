package com.bnpp.regliss.facade.mapper.external;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.external.facade.dto.record.*;
import com.bnpp.regliss.facade.dto.reference.ReferentialType;
import com.bnpp.regliss.facade.mapper.ReferenceResolverService;
import com.bnpp.regliss.profile.ReglissExternalAPIProfile;
import com.bnpp.regliss.repository.RecordAssociationRepository;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.repository.ReglementationRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.external.de.VersionProgramsService;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
@ReglissExternalAPIProfile
@Log
    public class RecordVigilanceSearchMapper {

    @Autowired
    private RecordAssociationRepository associationRepo;
    @Autowired
    private ReferenceResolverService referenceResolverService;
    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private RecordRepository recordRepo;
    @Autowired
    private ReglementationRepository reglementationRepository;
    @Autowired
    private VersionProgramsService versionProgramService;

    public RecordVigilanceDto toVigilanceSearchResultDto(Record record, Map<Long, ListVersionDTO> searchedVersions) {
        RecordVigilanceDto dto = mapCommonFields(record);
        ListVersionDTO recordVersion = searchedVersions.get(record.getVersionLinks().getListId());
        dto.generalFields.list = toListDtoV2(recordVersion);
        return dto;
    }

    public RecordVigilanceDto mapCommonFields(Record record) {
        RecordCommonVigilanceDto commonDto = toCommonRecordDto(record);
        return toTypeRelatedRecordDto(record, commonDto);
    }

    private RecordVigilanceDto toTypeRelatedRecordDto(Record record, RecordCommonVigilanceDto commonDto) {
        RecordVigilanceDto result;
        switch (record.getType()) {
        case PERSON:
            result = new RecordVigilanceDto(commonDto, toPersonDto((PersonRecord) record));
            break;
        case ENTITY:
            result = new RecordVigilanceDto(commonDto,
                    toEntityRecordDto((EntityRecord) record));
            break;
        case VESSEL:
            result = new RecordVigilanceDto(commonDto,
                    toVesselRecordDto((VesselRecord) record));
            break;
        case CITY:
            result = new RecordVigilanceDto(commonDto, toCityRecordDto());
            break;
        case COUNTRY:
            result = new RecordVigilanceDto(commonDto,
                    toCountryRecordDto());
            break;
        case AIRPORT:
            result = new RecordVigilanceDto(commonDto,
                    toAirportRecordDto());
            break;
        case SEAPORT:
            result = new RecordVigilanceDto(commonDto,
                    toSeaportRecordDto());
            break;
        case OTHER:
            result = new RecordVigilanceDto(commonDto, toOtherRecordDto());
            break;
        default:
            throw new ReglissException("Invalid record type: " + record.getType());
        }
        result.type = referenceService.translateEnum(record.getType(),Locale.ENGLISH);
        return result;
    }

    private PersonRecordVigilanceDto toPersonDto(PersonRecord record) {
        PersonRecordVigilanceDto personDto = new PersonRecordVigilanceDto();
        personDto.gendersLabel = record.getGenders().stream()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.gender, e.getId()))
                .collect(toList());
        personDto.title = record.getTitle();
        personDto.firstName = record.getFirstName();
        personDto.middleName = record.getMiddleName();
        personDto.lastName = record.getLastName();
        personDto.maidenName = record.getMaidenName();
        personDto.suffix = record.getSuffix();
        personDto.isDeceased = record.getDeceased();
        PersonCodesVigilanceDto codes = new PersonCodesVigilanceDto();
        codes.iban = record.getIbanCodes().stream().map(this::toIbanDto).collect(toSet());
        personDto.codes = codes;
        personDto.personImages = record.getPersonImages().stream().map(this::toPersonImageDto).collect(toSet());
        personDto.addresses = record.getAddresses().stream().sorted(comparing(Address::getId)).map(this::toAddressDto).collect(toList());
        personDto.identityDocuments = record.getIdentityDocuments().stream().map(this::toIdentityDocumentDto)
                .collect(toList());
        personDto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto)
                .collect(toList());
        personDto.personFunctions = record.getPersonFunctions().stream().map(this::toFunctionRecord).collect(toList());
        personDto.biography = record.getBiographies().stream().map(this::toBiographyDto).collect(toList());
        return personDto;
    }

    private EntityRecordVigilanceDto toEntityRecordDto(EntityRecord record) {
        EntityRecordVigilanceDto dto = new EntityRecordVigilanceDto();
        EntityCodesVigilanceDto codes = new EntityCodesVigilanceDto();
        codes.iban = record.getIbanCodes().stream().map(this::toIbanDto).collect(toSet());
        codes.bic = record.getBicCodes().stream().map(this::toBicDto).collect(toSet());
        codes.isin = record.getIsinCodes().stream().map(this::toIsinDto).collect(toSet());
        codes.cusip = record.getCusipCodes().stream().map(this::toCusipDto).collect(toSet());
        dto.codes=codes;
        dto.addresses = record.getAddresses().stream().sorted(comparing(Address::getId)).map(this::toAddressDto).collect(toList());
        dto.datesDetails = record.getDatesDetails().stream().map(this::toDateDetailsDto).collect(toList());
        dto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto).collect(toList());
        dto.identityDocuments =  Collections.emptyList();//not needed in search response
        return dto;
    }

    private CodeIbanVigilanceDto toIbanDto(CodeIban code) {
        CodeIbanVigilanceDto dto = new CodeIbanVigilanceDto();
        dto.id = code.getId();
        dto.code = code.getCode();
        return dto;
    }

    private PersonImageVigilanceDto toPersonImageDto(PersonImage personImage) {
        PersonImageVigilanceDto dto = new PersonImageVigilanceDto();
        dto.id = personImage.getId();
        dto.url = personImage.getUrl();
        return dto;
    }

    private CodeBicVigilanceDto toBicDto(CodeBic record) {
        CodeBicVigilanceDto dto = new CodeBicVigilanceDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }

    private CodeIsinVigilanceDto toIsinDto(CodeIsin record) {
        CodeIsinVigilanceDto dto = new CodeIsinVigilanceDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }

    private CodeCusipVigilanceDto toCusipDto(CodeCusip record) {
        CodeCusipVigilanceDto dto = new CodeCusipVigilanceDto();
        dto.id = record.getId();
        dto.code = record.getCode();
        return dto;
    }

    private VesselRecordVigilanceDto toVesselRecordDto(VesselRecord record) {
        VesselRecordVigilanceDto dto = new VesselRecordVigilanceDto();
        dto.imo = record.getImo();
        dto.flag = record.getFlag();
        dto.owner = record.getOwner();
        dto.vesselType = record.getVesselType();
        dto.tonnage = record.getTonnage();
        dto.grossTonnage = record.getGrossTonnage();
        dto.callSign = record.getCallSign();
        dto.datesDetails = Collections.emptyList();//not needed in search response
        dto.countryDetails = record.getCountriesDetails().stream().map(this::toCountryDetailsDto).collect(toList());
        dto.identityDocuments =  Collections.emptyList();//not needed in search response
        dto.addresses = record.getAddresses().stream().sorted(comparing(Address::getId)).map(this::toAddressDto).collect(toList());
        return dto;
    }

    private CityRecordVigilanceDto toCityRecordDto() {
        CityRecordVigilanceDto dto = new CityRecordVigilanceDto();
        dto.searchCode =  null;//not needed in search response
        return dto;
    }

    private AirportRecordVigilanceDto toAirportRecordDto() {
        AirportRecordVigilanceDto dto = new AirportRecordVigilanceDto();
        dto.searchCode = null;//not needed in search response
        return dto;
    }

    private SeaportRecordVigilanceDto toSeaportRecordDto() {
        SeaportRecordVigilanceDto dto = new SeaportRecordVigilanceDto();
        dto.searchCode = null;//not needed in search response
        return dto;
    }

    private CountryRecordVigilanceDto toCountryRecordDto() {
        CountryRecordVigilanceDto dto = new CountryRecordVigilanceDto();
        dto.searchCode = null;//not needed in search response
        dto.isoNum =  null;//not needed in search response
        dto.isoAlpha = null;//not needed in search response
        return dto;
    }

    private OtherRecordVigilanceDto toOtherRecordDto() {
        OtherRecordVigilanceDto dto = new OtherRecordVigilanceDto();
        dto.category =  null;//not needed in search response
        dto.searchCode =  null;//not needed in search response
        return dto;
    }

    public ListDto toListDtoV2(ListVersionDTO version) {
        ListDto listDto = new ListDto();
        listDto.list = version.getListName();
        listDto.publicationDate = version.getPublicationDate()!=null?version.getPublicationDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime():null;
        listDto.sourceVersion = version.getVersionNumber();
        listDto.applicationDate = version.getStartDate()!=null?version.getStartDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate():null;
        listDto.documentList = Collections.emptyList();//not needed in search response
        return listDto;
    }

    private RecordCommonVigilanceDto toCommonRecordDto(Record record) {
        RecordCommonVigilanceDto commonDto = new RecordCommonVigilanceDto();

        commonDto.name = record.getName();
        commonDto.description = null;//not needed in search response
        commonDto.keyword = null;//not needed in search response
        commonDto.notes = record.getDowJonesNotes();
        commonDto.reglissReference = record.getReglissReference();
        commonDto.externalReference = record.getExternalReference();
        commonDto.applicationDate = record.getStartDateStr();
        commonDto.remarks = record.getComments();
        commonDto.additionalInfo = record.getAdditionalInfo();
        commonDto.instruction = record.getInstruction();
        commonDto.recordId = record.getRecordId();
        commonDto.sanctionTypesLabel = Collections.emptyList();//not needed in search response

        commonDto.programs = record.getPrograms().stream()
                                                     .map(ProgramVigilanceDto::new)
                                                     .collect(toList());
        commonDto.aliases = mapAliasesBasedOnRecordType(record);
        commonDto.sanctionReferences = Collections.emptyList();//not needed in search response
        commonDto.sources = record.getSources().stream().map(this::toRecordSourceDto).collect(toList());
        commonDto.relations = Collections.emptyList();//not needed in search response
        commonDto.categoriesLabel = record.getCategories().stream().sorted(comparing(RecordReferential::getLabel))
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.category, e.getId()))
                .collect(toList());

        return commonDto;
    }

    private List<AliasVigilanceDto> mapAliasesBasedOnRecordType(Record record) {
        Function<Alias, AliasVigilanceDto> aliasMappingFunction;
        if (needsFullNameMapping(record)) {
            aliasMappingFunction = this::toAliasDtoOtherThanPerson;
        } else {
            aliasMappingFunction = this::toAliasDto;
        }
        return record.getAliases().stream().sorted(comparing(Alias::getReglissReference)).map(aliasMappingFunction)
                .collect(toList());
    }

    private boolean needsFullNameMapping(Record record) { return record.getType() != RecordType.PERSON; }

    private AliasVigilanceDto toAliasDto(Alias alias) {
        AliasVigilanceDto aliasDto = toBaseAlias(alias);
        aliasDto.lastName = alias.getLastName();
        return aliasDto;

    }

    private AliasVigilanceDto toAliasDtoOtherThanPerson(Alias alias) {
        AliasVigilanceDto aliasDto = toBaseAlias(alias);
        aliasDto.lastName = alias.getFullName();
        return aliasDto;
    }

    private AliasVigilanceDto toBaseAlias(Alias alias) {
        AliasVigilanceDto aliasDto = new AliasVigilanceDto();
        aliasDto.id = alias.getId();
        aliasDto.reglissReference = alias.getReglissReference();
        aliasDto.title = alias.getTitle();
        aliasDto.firstName = alias.getFirstName();
        aliasDto.middleName = alias.getMiddleName();
        aliasDto.typeLabel = alias.getTypeOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.aliasType, e.getId()))
                .orElse(null);
        aliasDto.qualityLabel = alias.getQualityOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.aliasQuality, e.getId()))
                .orElse(null);
        aliasDto.language = alias.getLanguage();
        return aliasDto;
    }

    private RecordSourceVigilanceDto toRecordSourceDto(RecordSource source) {
        RecordSourceVigilanceDto recordSourceDto = new RecordSourceVigilanceDto();
        recordSourceDto.id = source.getId();
        recordSourceDto.name = source.getName();
        return recordSourceDto;
    }

    private CountryDetailsVigilanceDto toCountryDetailsDto(CountryDetails countryDetails) {
        CountryDetailsVigilanceDto countryDetailsDto = new CountryDetailsVigilanceDto();
        countryDetailsDto.id = countryDetails.getId();
        countryDetailsDto.additionalInfo = countryDetails.getAdditionalInfoOpt().orElse(null);
        countryDetailsDto.countryLabel = countryDetails.getCountryOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.country, e.getId())).orElse(null);
        countryDetailsDto.principal = countryDetails.getPrimary();
        countryDetailsDto.typeLabel = countryDetails.getTypeOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.countryType, e.getId()))
                .orElse(null);
        return countryDetailsDto;
    }

    private PersonFunctionVigilanceDto toFunctionRecord(PersonFunction entity) {
        PersonFunctionVigilanceDto dto = new PersonFunctionVigilanceDto();
        dto.id = entity.getId();
        dto.from = entity.getFromStr();
        dto.to = entity.getToStr();
        dto.functionName = entity.getFunctionName();
        dto.pepOccupationCategoryLabel = entity.getPepOccupationCategory() != null
                ? referenceResolverService.resolveReferential(ReferentialType.pepOccupationCategory,
                entity.getPepOccupationCategory().getId())
                : null;
        return dto;
    }

    private PersonBiographyVigilanceDto toBiographyDto(PersonBiography biography) {
        PersonBiographyVigilanceDto biographyDto = new PersonBiographyVigilanceDto();
        biographyDto.id = biography.getId();
        biographyDto.dateValue = biography.getDateValueStrOpt().orElse(null);
        biographyDto.dateTypeLabel = biography.getDateTypeOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.dateType, e.getId())).orElse(null);
        biographyDto.birthPlace = biography.getBirthPlaceOpt().orElse(null);
        biographyDto.countryLabel = biography.getBirthCountryOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.country, e.getId())).orElse(null);
        biographyDto.additionalInfo = biography.getAdditionalInfo();
        return biographyDto;
    }

    private DateDetailsVigilanceDto toDateDetailsDto(DateDetails dateDetails) {
        DateDetailsVigilanceDto dateDetailsDto = new DateDetailsVigilanceDto();
        dateDetailsDto.id = dateDetails.getId();
        dateDetailsDto.dateValue = dateDetails.getDateValueStr();
        dateDetailsDto.dateTypeLabel = dateDetails.getDateTypeOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.dateType, e.getId())).orElse(null);
        return dateDetailsDto;
    }

    private AddressVigilanceDto toAddressDto(Address address) {
        AddressVigilanceDto addressDto = new AddressVigilanceDto();
        addressDto.id = address.getId();
        addressDto.address = address.getAddress();
        addressDto.zipCode = address.getZipCode();
        addressDto.city = address.getCity();
        addressDto.state = address.getState();
        addressDto.countryLabel = address.getCountryOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.country, e.getId())).orElse(null);
        addressDto.remarks = address.getRemarks();
        return addressDto;
    }

    private IdentityDocumentVigilanceDto toIdentityDocumentDto(IdentityDocument identityDocument) {
        IdentityDocumentVigilanceDto identityDocumentDto = new IdentityDocumentVigilanceDto();
        identityDocumentDto.id = identityDocument.getId();
        identityDocumentDto.additionalInfo = identityDocument.getAdditionalInfo();
        identityDocumentDto.issuingDate = identityDocument.getIssuingDateStr();
        identityDocumentDto.expirationDate = identityDocument.getExpirationDateStr();
        identityDocumentDto.countryLabel = identityDocument.getCountryOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.country, e.getId())).orElse(null);
        identityDocumentDto.number = identityDocument.getNumberOpt().orElse(null);
        identityDocumentDto.typeLabel = identityDocument.getTypeOpt()
                .map(e -> referenceResolverService.resolveReferential(ReferentialType.identityDocumentType, e.getId()))
                .orElse(null);

        return identityDocumentDto;
    }

    public void setReferenceResolverService(ReferenceResolverService referenceResolverService) {
        this.referenceResolverService = referenceResolverService;
    }
}
