package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.record.*;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.SanctionType;
import com.bnpp.regliss.facade.dto.record.RecordSearchResultDto;
import com.bnpp.regliss.facade.dto.version.RecordInVersionSearchResultDto;
import com.bnpp.regliss.profile.ReglissProfile;
import com.bnpp.regliss.vo.RecordAndVersionForSearchVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

@Component
@Profile({ReglissProfile.REGLISS_WEB, ReglissProfile.REGLISS_EXTERNAL_API})
public class RecordSearchMapper {

    private static final String CODE =  "CODE: ";

    public RecordInVersionSearchResultDto toRecordSearchForVersionDto(Record record, Version version){
        RecordInVersionSearchResultDto dto = new RecordInVersionSearchResultDto();
        if(record!=null && record.getId()!=null) {
            dto.categories = record.getCategories().stream().map(RecordReferential::getId).collect(toSet());
            dto.code = getRecordCodesStr(record);
            dto.id = record.getExternalUuid();
            dto.name = mapRecordName(record);

            dto.program = record.getPrograms().stream()
                    .map(pr -> pr.getLabelOpt().orElse(""))
                    .distinct()
                    .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                    .collect(joining(", "));
            dto.reference = record.getReglissReference();
            dto.type = record.getType();
            dto.listConfidentiality = version.getList().getConfidentiality();
        }
        return dto;
    }

    private String mapRecordName(Record record) { return NameMappingUtil.toUppercaseLastName(record.getName(),record); }


    public RecordSearchResultDto toRecordSearchDto(RecordAndVersionForSearchVO recordVersion){
        RecordSearchResultDto dto = new RecordSearchResultDto();
        Record record = recordVersion.getRecord();
        dto.isActive = recordVersion.isActive();
        dto.listName = recordVersion.getListShortName();
        dto.listConfidentiality = recordVersion.getListConfidentiality();
        dto.fromVersionNumber = recordVersion.getFromVersionNumber().toString();
        dto.fromVersionId = recordVersion.getFromVersionId();
        dto.toVersionNumber = recordVersion.getToVersionNumber().toString();
        dto.toVersionId = recordVersion.getToVersionId();
        if(recordVersion.getDeletedInVersionId()!=null) {
            dto.deletedInVersionNumber =String.valueOf(recordVersion.getToVersionNumber()+1);
        }
        dto.recordType = record.getType();
        dto.id = record.getExternalUuid();
        dto.nameCodeIban = StringUtils.defaultString(mapRecordName(record));

        if (isNotEmpty(getRecordCodesStr(record))) {
            dto.nameCodeIban += " /\n" + getRecordCodesStr(record);
        }
        dto.sanctionTypes = record.getSanctionTypes().stream()
                .map(SanctionType::getLabel)
                .sorted(Comparator.naturalOrder())
                .collect(joining(", "));
        dto.programs = record.getPrograms().stream()
                .map(pr->pr.getLabelOpt().orElse(""))
                .distinct()
                .sorted()
                .collect(joining(", "));
        dto.reglissReference = record.getReglissReference();

        return dto;
    }

    private String getRecordCodesStr(Record record) { return String.join(";\n",getRecordCodes(record)); }

    private List<String> getRecordCodes(Record record) {
        if (record instanceof EntityRecord) {
            return getRecordCodesForEntity((EntityRecord) record);
        }
        if (record instanceof PersonRecord) {
            return getRecordCodesForPerson((PersonRecord) record);
        }
        if (record instanceof VesselRecord) {
            return getRecordCodesForVessel((VesselRecord) record);
        }
        if (record instanceof AirportRecord) {
            return getRecordCodesForAirport((AirportRecord) record);
        }
        if (record instanceof OtherRecord) {
            return getRecordCodesForOther((OtherRecord) record);
        }
        if (record instanceof CountryRecord) {
            return getRecordCodesForCountry((CountryRecord) record);
        }
        if (record instanceof CityRecord) {
            return getRecordCodesForCity((CityRecord) record);
        }
        if (record instanceof SeaportRecord) {
            return getRecordCodesForSeaport((SeaportRecord) record);
        }
        throw new IllegalArgumentException("Not implemented");
    }

    private List<String> getRecordCodesForEntity(EntityRecord entity) {
        List<String> strings = new ArrayList<>();
        if (!entity.getIbanCodes().isEmpty()) {
            strings.add("IBAN: " + entity.getIbanCodes().stream().map(CodeIban::getCode).collect(joining(",")));
        }
        if (!entity.getBicCodes().isEmpty()) {
            strings.add("BIC: " + entity.getBicCodes().stream().map(CodeBic::getCode).collect(joining(",")));
        }
        if (!entity.getIsinCodes().isEmpty()) {
            strings.add("ISIN: " + entity.getIsinCodes().stream().map(CodeIsin::getCode).collect(joining(",")));
        }
        if (!entity.getCusipCodes().isEmpty()) {
            strings.add("CUSIP: " + entity.getCusipCodes().stream().map(CodeCusip::getCode).collect(joining(",")));
        }
        return strings;
    }

    private List<String> getRecordCodesForPerson(PersonRecord person) {
        if (!person.getIbanCodes().isEmpty()) {
            return Collections.singletonList("IBAN: " + person.getIbanCodes().stream().map(CodeIban::getCode).collect(joining(",")));
        } else {
            return emptyList();
        }
    }

    private List<String> getRecordCodesForVessel(VesselRecord vessel) {
        if (vessel.getImo() != null) {
            return Collections.singletonList("IMO: " + vessel.getImo());
        } else {
            return emptyList();
        }
    }
    private List<String> getRecordCodesForAirport(AirportRecord airport) {
        if (airport.getSearchCode() != null) {
            return Collections.singletonList(CODE + airport.getSearchCode());
        } else {
            return emptyList();
        }
    }
    private List<String> getRecordCodesForCity(CityRecord city) {
        if (city.getSearchCode() != null) {
            return Collections.singletonList(CODE + city.getSearchCode());
        } else {
            return emptyList();
        }
    }

    private List<String> getRecordCodesForCountry(CountryRecord country) {
        if (country.getSearchCode() != null) {
            return Collections.singletonList(CODE + country.getSearchCode());
        } else {
            return emptyList();
        }
    }

    private List<String> getRecordCodesForOther(OtherRecord other) {
        if (other.getSearchCode() != null) {
            return Collections.singletonList(CODE + other.getSearchCode());
        } else {
            return emptyList();
        }
    }

    private List<String> getRecordCodesForSeaport(SeaportRecord seaport) {
        if (seaport.getSearchCode() != null) {
            return Collections.singletonList(CODE + seaport.getSearchCode());
        } else {
            return emptyList();
        }
    }
}

