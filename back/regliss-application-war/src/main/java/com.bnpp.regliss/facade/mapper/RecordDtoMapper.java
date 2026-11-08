package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.facade.dto.record.*;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.repository.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;


@Component
@Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class RecordDtoMapper {
    @Autowired
    private CountryRepository countryRepo;

    @Autowired
    private DateTypeRepository dateTypeRepo;

    @Autowired
    private CountryTypeRepository countryTypeRepo;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;

    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    @Autowired
    private Description3Repository description3Repository;

    @Autowired
    private IdentityDocumentTypeRepository identityDocumentRepo;

    @Autowired
    private RelationTypeRepository relationRepo;

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private PEPOccupationCategoryRepository pepOccupationCategoryRepo;

    @Autowired
    private GenderRepository genderRepository;

    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepo;

    @Autowired
    private AliasTypeRepository aliasTypeRepo;

    @Autowired
    private AliasQualityRepository aliasQualityRepo;

    /*unchecked*/
    public <T extends Record > T toEntity(RecordDto dto){
        switch (dto.type) {
        case PERSON: return (T) fillPersonRecord(new PersonRecord(), dto);
        case ENTITY: return (T) fillEntityRecord(new EntityRecord(), dto);
        case VESSEL: return (T) fillVesselRecord(new VesselRecord(), dto);
        case CITY: return (T) fillCityRecord(new CityRecord(), dto);
        case COUNTRY: return (T) fillCountryRecord(new CountryRecord(), dto);
        case AIRPORT: return (T) fillAirportRecord(new AirportRecord(), dto);
        case SEAPORT: return (T) fillSeaportRecord(new SeaportRecord(), dto);
        case OTHER: return (T) fillOtherRecord(new OtherRecord(), dto);
        default:
            throw new ReglissException("Invalid record type: " + dto.type);
        }
    }

    private void fillCommonRecordField(Record record, RecordDto dto) {

        Optional<Record> recordToBeUpdated = recordRepo.findByUUIDAndVersionId(dto.id, dto.generalFields.idVersion);
        recordToBeUpdated.ifPresent(value -> record.setId(value.getId()));
        record.setName(dto.generalFields.name);
        record.setDescription(dto.generalFields.description);
        record.setKeywords(dto.generalFields.keywords);
        record.setDowJonesNotes(dto.generalFields.notes);
        record.setExternalReference(dto.generalFields.externalReference);
        record.setStartDateStr(dto.generalFields.startDate);
        record.setComments(dto.generalFields.comments);
        record.setAdditionalInfo(dto.generalFields.additionalInfo);
        record.setInstruction(dto.generalFields.instruction);
        record.setRecordId(dto.generalFields.recordId);
        record.setSanctionTypes(new HashSet<>(sanctionTypeRepo.findAllById(dto.generalFields.sanctionTypes)));
        record.setPrograms(dto.generalFields.programs.stream().map(this::toProgram).collect(Collectors.toSet()));
        record.setSanctionReferences(dto.generalFields.sanctionReferences.stream().map(this::toSanctionReference)
                .collect(Collectors.toSet()));
        record.setSources(dto.generalFields.sources.stream().map(this::toRecordSource).collect(Collectors.toSet()));
        record.setRelations(dto.generalFields.relations.stream().map(this::toRecordRelation).collect(Collectors.toSet()));
        record.setCategories(new HashSet<>(recordCategoryRepo.findAllById(dto.generalFields.categories)));
        record.setDescriptions3(new HashSet<>(description3Repository.findAllById(dto.generalFields.descriptions)));
        record.setAliases(dto.generalFields.aliases.stream().map(this::toAliasNonPerson).collect(Collectors.toSet()));
        record.setReglissReference(dto.generalFields.reglissReference);
    }

    private PersonRecord fillPersonRecord(PersonRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        PersonRecordDto personDto = dto.personFields;
        record.setGenders(new HashSet<>(genderRepository.findAllById(personDto.genders)));
        record.setTitle(personDto.title);
        record.setFirstName(personDto.firstName);
        record.setMiddleName(personDto.middleName);
        record.setLastName(personDto.lastName);
        record.setMaidenName(personDto.maidenName);
        record.setSuffix(personDto.suffix);
        record.setDeceased(personDto.isDeceased);
        record.setIbanCodes(personDto.iban.stream().map(this::toIban).collect(toSet()));
        record.setPersonImages(personDto.personImages.stream().map(this::toPersonImage).collect(toSet()));
        record.setAddresses(personDto.addresses.stream().map(this::toAddress).collect(toSet()));
        record.setIdentityDocuments(personDto.identityDocuments.stream().map(this::toIdentityDocument).collect(toSet()));
        record.setCountriesDetails(personDto.countryDetails.stream().map(this::toNationality).collect(Collectors.toSet()));
        record.setBiographies(personDto.biography.stream().map(this::toPersonBiography).collect(Collectors.toSet()));
        record.setPersonFunctions(personDto.personFunctions.stream().map(this::toPersonFunction).collect(Collectors.toSet()));
        return record;
    }

    private Address toAddress(AddressDto addressDto) {
        Address address = new Address();
        address.setId(addressDto.id);
        address.setAddress(addressDto.address);
        address.setZipCode(addressDto.zipCode);
        address.setCity(addressDto.city);
        address.setState(addressDto.state);
        address.setAdditionalAddress(addressDto.additionalAddress);
        address.setRemarks(addressDto.remarks);
        address.setReglissReference(addressDto.reglissReference);

        if (addressDto.country != null) {
            address.setCountry(countryRepo.getExactlyOne(addressDto.country));
        }
        return address;
    }

    private DateDetails toDateDetails(DateDetailsDto dateDetailsDto) {
        DateDetails dateDetails = new DateDetails();
        dateDetails.setId(dateDetailsDto.id);
        dateDetails.setDateValueStr(dateDetailsDto.dateValue);
        if (dateDetailsDto.dateType != null) {
            dateDetails.setDateType(dateTypeRepo.getExactlyOne(dateDetailsDto.dateType));
        }
        return dateDetails;
    }

    private Alias toAlias(AliasDto aliasDto) {
        Alias alias = new Alias();
        alias.setId(aliasDto.id);
        alias.setExternalReference(aliasDto.externalReference);
        alias.setTitle(aliasDto.title);
        alias.setFirstName(aliasDto.firstName);
        alias.setLastName(aliasDto.lastName);
        alias.setFullName(aliasDto.lastName);
        alias.setMiddleName(aliasDto.middleName);
        alias.setSuffix(aliasDto.suffix);
        if(aliasDto.quality != null) {
            alias.setQuality(aliasQualityRepo.getExactlyOne(aliasDto.quality));
        }
        if(aliasDto.type != null) {
            alias.setType(aliasTypeRepo.getExactlyOne(aliasDto.type));
        }
        if(StringUtils.isNotEmpty(aliasDto.reglissReference)) {
            alias.setReglissReference(aliasDto.reglissReference);
        }
        alias.setLanguage(aliasDto.language);

        return alias;
    }

    private Alias toAliasNonPerson(AliasDto aliasDto) {
        Alias alias=this.toAlias(aliasDto);
        alias.setFullName(alias.getComputedFullName());
        alias.setSuffix(aliasDto.suffix);
        return alias;
    }

    private SanctionReference toSanctionReference(SanctionReferenceDto sanctionReferenceDto) {
        SanctionReference sanctionReference = new SanctionReference();
        sanctionReference.setId(sanctionReferenceDto.id);
        sanctionReference.setRemarks(sanctionReferenceDto.remarks);
        sanctionReference.setListName(sanctionReferenceDto.name);
        sanctionReference.setSinceDateStr(sanctionReferenceDto.sinceDate);
        sanctionReference.setEndDateStr(sanctionReferenceDto.endDate);
        return sanctionReference;
    }

    private RecordSource toRecordSource(RecordSourceDto recordSourceDto) {
        RecordSource source = new RecordSource();
        source.setId(recordSourceDto.id);
        source.setName(recordSourceDto.name);
        return source;
    }

    private RecordRelation toRecordRelation(RecordRelationDto recordRelationDto) {
        RecordRelation recordRelation = new RecordRelation();
        recordRelation.setId(recordRelationDto.id);
        if (recordRelationDto.category != null)
            recordRelation.setCategory(recordCategoryRepo.getExactlyOne(recordRelationDto.category));
        recordRelation.setName(recordRelationDto.name);
        recordRelation.setRecordType(recordRelationDto.type);
        recordRelation.setEx(recordRelationDto.ex);
        if (recordRelationDto.relationTypeId != null)
            recordRelation.setRelationType(relationRepo.getExactlyOne(recordRelationDto.relationTypeId));
        return recordRelation;
    }

    private CountryDetails toNationality(CountryDetailsDto nationalityDto) {
        CountryDetails nationality = new CountryDetails();
        nationality.setId(nationalityDto.id);
        nationality.setAdditionalInfo(nationalityDto.additionalInfo);
        if(nationalityDto.country!=null)nationality.setCountry(countryRepo.getExactlyOne(nationalityDto.country));
        nationality.setPrimary(nationalityDto.principal);
        if(nationalityDto.type!=null)nationality.setType(countryTypeRepo.getExactlyOne(nationalityDto.type));
        return nationality;
    }


    private PersonFunction toPersonFunction(PersonFunctionDto dto) {
        PersonFunction entity = new PersonFunction();
        entity.setId(dto.id);
        entity.setFromStr(dto.from);
        entity.setToStr(dto.to);
        entity.setFunctionName(dto.functionName);
        if (dto.pepOccupationCategory != null)
            entity.setPepOccupationCategory(pepOccupationCategoryRepo.getExactlyOne(dto.pepOccupationCategory));
        return entity;
    }

    private PersonBiography toPersonBiography(PersonBiographyDto dateDetailsDto) {
        PersonBiography dateDetails = new PersonBiography();
        dateDetails.setId(dateDetailsDto.id);
        dateDetails.setDateValueStr(dateDetailsDto.dateValue);
        if(dateDetailsDto.dateType!=null) {
            dateDetails.setDateType(dateTypeRepo.getExactlyOne(dateDetailsDto.dateType));
        }
        dateDetails.setBirthPlace(dateDetailsDto.birthPlace);
        if (dateDetailsDto.country != null) {
            dateDetails.setBirthCountry(countryRepo.getExactlyOne(dateDetailsDto.country));
        }
        dateDetails.setAdditionalInfo(dateDetailsDto.additionalInfo);
        dateDetails.setPrimary(dateDetailsDto.principal);
        return dateDetails;
    }

    private Program toProgram(ProgramDto programDto) {
        Program program = new Program();
        program.setId(programDto.id);
        program.setDescription(programDto.description);
        program.setLabel(programDto.label);
        if (programDto.sanctionType != null) {
            program.setSanctionType(programSanctionTypeRepo.getExactlyOne(programDto.sanctionType));
        }
        return program;
    }

    private IdentityDocument toIdentityDocument(IdentityDocumentDto identityDocumentDto) {
        IdentityDocument identityDocument = new IdentityDocument();
        identityDocument.setId(identityDocumentDto.id);
        identityDocument.setAdditionalInfo(identityDocumentDto.additionalInfo);
        if (identityDocumentDto.country != null) {
            identityDocument.setCountry(countryRepo.getExactlyOne(identityDocumentDto.country));
        }
        identityDocument.setIssuingDateStr(identityDocumentDto.issuingDate);
        identityDocument.setExpirationDateStr(identityDocumentDto.expirationDate);
        identityDocument.setNumber(identityDocumentDto.number);
        if (identityDocumentDto.type != null) {
            identityDocument.setType(identityDocumentRepo.getExactlyOne(identityDocumentDto.type));
        }
        return identityDocument;
    }

    private EntityRecord fillEntityRecord(EntityRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setIbanCodes(dto.entityFields.iban.stream().map(this::toIban).collect(Collectors.toSet()));
        record.setBicCodes(dto.entityFields.bic.stream().map(this::toBic).collect(Collectors.toSet()));
        record.setIsinCodes(dto.entityFields.isin.stream().map(this::toIsin).collect(Collectors.toSet()));
        record.setCusipCodes(dto.entityFields.cusip.stream().map(this::toCusip).collect(Collectors.toSet()));
        record.setAddresses(dto.entityFields.addresses.stream().map(this::toAddress).collect(Collectors.toSet()));
        record.setCountriesDetails(dto.entityFields.countryDetails.stream().map(this::toNationality).collect(Collectors.toSet()));
        record.setDateDetails(dto.entityFields.datesDetails.stream().map(this::toDateDetails).collect(Collectors.toSet()));
        record.setIdentityDocuments(dto.entityFields.identityDocuments.stream().map(this::toIdentityDocument).collect(toSet()));
        return record;
    }
    private CodeIban toIban(CodeIbanDto dto) {
        CodeIban code = new CodeIban();
        code.setId(dto.id);
        code.setCode(dto.code);
        return code;
    }
    private PersonImage toPersonImage(PersonImageDto dto) {
        PersonImage personImage = new PersonImage();
        personImage.setId(dto.id);
        personImage.setUrl(dto.url);
        return personImage;
    }
    private CodeBic toBic(CodeBicDto dto) {
        CodeBic code = new CodeBic();
        code.setId(dto.id);
        code.setCode(dto.code);
        return code;
    }
    private CodeIsin toIsin(CodeIsinDto dto) {
        CodeIsin code = new CodeIsin();
        code.setId(dto.id);
        code.setCode(dto.code);
        return code;
    }
    private CodeCusip toCusip(CodeCusipDto dto) {
        CodeCusip code = new CodeCusip();
        code.setId(dto.id);
        code.setCode(dto.code);
        return code;
    }

    private VesselRecord fillVesselRecord(VesselRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setImo(dto.vesselFields.imo);
        record.setFlag(dto.vesselFields.flag);
        record.setOwner(dto.vesselFields.owner);
        record.setVesselType(dto.vesselFields.vesselType);
        record.setTonnage(dto.vesselFields.tonnage);
        record.setGrossTonnage(dto.vesselFields.grossTonnage);
        record.setCountriesDetails(dto.vesselFields.countryDetails.stream().map(this::toNationality).collect(Collectors.toSet()));
        record.setDateDetails(dto.vesselFields.datesDetails.stream().map(this::toDateDetails).collect(Collectors.toSet()));
        record.setIdentityDocuments(dto.vesselFields.identityDocuments.stream().map(this::toIdentityDocument).collect(toSet()));

        return record;
    }

    private CityRecord fillCityRecord(CityRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setSearchCode(dto.cityFields.searchCode);

        return record;
    }

    private CountryRecord fillCountryRecord(CountryRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setSearchCode(dto.countryFields.searchCode);
        record.setIsoNum(dto.countryFields.isoNum);
        record.setIsoAlpha(dto.countryFields.isoAlpha);

        return record;
    }

    private AirportRecord fillAirportRecord(AirportRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setSearchCode(dto.airportFields.searchCode);
        return record;
    }

    private SeaportRecord fillSeaportRecord(SeaportRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setSearchCode(dto.seaportFields.searchCode);

        return record;
    }

    private OtherRecord fillOtherRecord(OtherRecord record, RecordDto dto) {
        fillCommonRecordField(record, dto);
        record.setOtherCategory(dto.otherFields.category);
        record.setSearchCode(dto.otherFields.searchCode);

        return record;
    }


}
