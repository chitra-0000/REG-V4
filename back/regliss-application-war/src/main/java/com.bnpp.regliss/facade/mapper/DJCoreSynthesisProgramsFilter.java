package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.record.Program;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.bnpp.regliss.entity.referential.ReferentialBusinessConcept.*;
import static java.util.stream.Collectors.toList;

public class DJCoreSynthesisProgramsFilter {
    public static final List<ReferentialBusinessConcept> EXCLUDED_PROGRAMS = Collections.unmodifiableList(Stream.of(
            PROGRAM_TYPE_OFAC_UNIQUE_ID,
            PROGRAM_TYPE_EU_CONSOLIDATED_ELECTRONIC_LIST_ID,
            PROGRAM_TYPE_UN_PERMANENT_REFERENCE_NO,
            PROGRAM_TYPE_HM_TREASURY_GROUP_ID,
            PROGRAM_TYPE_RELATED_OFAC_UNIQUE_ID,
            PROGRAM_TYPE_RELATED_EU_CONSOLIDATED_ELECTRONIC_LIST_ID,
            PROGRAM_TYPE_SECO_SSID,
            PROGRAM_TYPE_DFAT_REFERENCE_NUMBER,
            PROGRAM_TYPE_OSFI_INDIVIDUALS_ID,
            PROGRAM_TYPE_OSFI_ENTITIES_ID,
            PROGRAM_TYPE_OSFI_IRAN_ID,
            PROGRAM_TYPE_OSFI_NORTH_KOREA_ID,
            PROGRAM_TYPE_OFAC_ADDITIONAL_SANCTIONS_INFORMATION
            ).collect(toList()));

    public static boolean isProgramIncluded(Program program) { return !isProgramExcluded(program); }
    public static boolean isProgramExcluded(Program program){
        return program.getSanctionTypeOpt()
                .map( st-> st.getBusinessConceptOpt()
                                .map(EXCLUDED_PROGRAMS::contains)
                                .orElse(false))
                .orElse(false);
    }

    private DJCoreSynthesisProgramsFilter(){}
}
