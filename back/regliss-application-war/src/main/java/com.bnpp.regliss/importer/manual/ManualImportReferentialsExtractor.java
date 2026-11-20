package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.*;
import com.bnpp.regliss.service.ReferenceService;
import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@ReglissBatchProfile
public class ManualImportReferentialsExtractor {
    private static final Logger log = LoggerFactory.getLogger(ManualImportReferentialsExtractor.class);

    @Autowired
    private Description3Repository description3Repo;
    @Autowired
    private SanctionTypeRepository sanctionTypeRepo;
    @Autowired
    private PEPOccupationCategoryRepository occupationRepo;
    @Autowired
    private DateTypeRepository dateTypeRepo;
    @Autowired
    private CountryTypeRepository countryTypeRepo;
    @Autowired
    private IdentityDocumentTypeRepository identityDocumentTypeRepo;
    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepo;
    @Autowired
    private RecordCategoryRepository recordCategoryRepo;
    @Autowired
    private CountryRepository countryRepo;
    @Autowired
    private GenderRepository genderRepo;
    @Autowired
    private RelationTypeRepository relationTypeRepo;
    @Autowired
    private AliasTypeRepository aliasTypeRepo;
    @Autowired
    private AliasQualityRepository aliasQualityRepo;
    @Autowired
    private ReferenceService referenceService;

    @Cacheable(cacheNames = "threadCache", key = "#root.methodName")
    public Map<AuditedFieldCode, Map<String, Object>> getManualImportKeysToValuesMap() {
        log.debug("Fetching All Record Labels from DB");
        Map<AuditedFieldCode, Map<String, Object>> map = new EnumMap<>(AuditedFieldCode.class);
        map.put(AuditedFieldCode.RECORD_CATEGORY, getManualRefEntries(recordCategoryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_SANCTION_TYPE, getManualRefEntries(sanctionTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_DESCRIPTION3, getManualRefEntries(description3Repo.findAll()));
        map.put(AuditedFieldCode.PERSON_GENDER, getManualRefEntries(genderRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_COUNTRY_DETAILS_TYPE, getManualRefEntries(countryTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY, getManualRefEntries(countryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_DATE_DETAILS_DATE_TYPE, getManualRefEntries(dateTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_ADDRESS_COUNTRY, getManualRefEntries(countryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_COUNTRY, getManualRefEntries(countryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE, getManualRefEntries(identityDocumentTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_DATE_TYPE, getManualRefEntries(dateTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_BIRTH_COUNTRY, getManualRefEntries(countryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE, getManualRefEntries(programSanctionTypeRepo.findAll()));
        map.put(AuditedFieldCode.PERSON_FUNCTION_CATEGORY, getManualRefEntries(occupationRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_RELATION_CATEGORY, getManualRefEntries(recordCategoryRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_RELATION_TYPE, getManualRefEntries(relationTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_ALIAS_TYPE, getManualRefEntries(aliasTypeRepo.findAll()));
        map.put(AuditedFieldCode.RECORD_ALIAS_QUALITY, getManualRefEntries(aliasQualityRepo.findAll()));

        map.put(AuditedFieldCode.RECORD_TYPE, getEnumEntries(RecordType.class));
        map.put(AuditedFieldCode.RECORD_RELATION_RECORD_TYPE, getEnumEntries(RecordType.class));

        map.put(AuditedFieldCode.RECORD_COUNTRY_DETAILS_IS_PRIMARY, booleanResolver());
        map.put(AuditedFieldCode.RECORD_RELATION_EX, booleanResolver());
        map.put(AuditedFieldCode.PERSON_IS_DECEASED, booleanResolver());
        map.put(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_IS_PRIMARY, booleanResolver());

        return map;
    }

    public String getAliasQualityStrongLabel() {
        return aliasQualityRepo.findByBusinessConcept(ReferentialBusinessConcept.ALIAS_QUALITY_STRONG).getLabel();
    }

    /unchecked/
    private Map<String, Object> getManualRefEntries(List<? extends RecordReferential> all) {
        Map<String, Object> result = new HashMap<>();
        List<? extends RecordReferential> referentialsToBeIncluded = all.stream()
                .filter(r -> !r.isV3Only()).collect(toList());

        List<? extends RecordReferential> defaultValues = referentialsToBeIncluded.stream()
                .filter(r -> r.hasBusinessConcept(ReferentialBusinessConcept.DEFAULT_VALUE))
                .collect(toList());
        result.putAll(defaultValues.stream().collect(toMap(l -> ReferentialBusinessConcept.DEFAULT_VALUE.name(), identity(), (x, y) -> x)));

        List<? extends RecordReferential> referentialsNotDefault = ListUtils.removeAll(referentialsToBeIncluded, defaultValues);
        result.putAll(referentialsNotDefault.stream().collect(toMap(RecordReferential::getLabel, identity(), (x, y) -> x)));

        return result;
    }

    private <E extends Enum<E>> Map<String, Object> getEnumEntries(Class<E> enumClass) {
        Map<String, Object> map = new HashMap<>();
        for(E e : enumClass.getEnumConstants()){
            map.put(referenceService.translateEnum(e, Locale.ENGLISH), e);
            map.put(referenceService.translateEnum(e, Locale.FRANCE), e);
            map.put(e.name(), e);
        }
        return map;
    }

    private Map<String, Object> booleanResolver() {
        Map<String, Object> map = new HashMap<>();
        map.put("yes", true);
        map.put("Yes", true);
        map.put("YES", true);
        map.put("True", true);
        map.put("true", true);
        map.put("TRUE", true);

        map.put("no", false);
        map.put("No", false);
        map.put("NO", false);
        map.put("false", false);
        map.put("False", false);
        map.put("FALSE", false);
        return map;
    }

}
