package com.bnpp.regliss.service;

import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.stereotype.*;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.entity.referential.*;
import com.bnpp.regliss.entity.referential.record.*;
import com.bnpp.regliss.repository.*;

@Service
public class ReferenceService {

    @Autowired
    private RegulatorOrInternalPolicyRepository regulatorOrInternalPolicyRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ListCategoryRepository listCategoryRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private SanctionTypeRepository sanctionTypeRepository;

    @Autowired
    private ProgramSanctionTypeRepository programSanctionTypeRepository;

    @Autowired
    private RecordCategoryRepository recordCategoryRepository;

    @Autowired
    private IdentityDocumentTypeRepository identityDocumentTypeRepository;

    // TODO: [GAP - field(s) between lines 41-42 not captured in screenshots]
    private GeographicDataRepository geographicDataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RelationTypeRepository relationTypeRepository;

    @Autowired
    private DateTypeRepository dateTypeRepository;

    @Autowired
    private CountryTypeRepository countryTypeRepository;

    @Autowired
    private AliasTypeRepository aliasTypeRepository;

    @Autowired
    private AliasQualityRepository aliasQualityRepository;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ReglissRequestContext requestContext;

    public User getSysUser() {
        User sysUser = userRepository.findByUsername(User.SYS_USERNAME);
        if (sysUser == null) {
            throw new IllegalStateException("No sys user with username '" + User.SYS_USERNAME + "'");
        }
        return sysUser;
    }

    public RegulatorOrInternalPolicy getRegulatorOrInternalPolicyById(Long id) {
        return regulatorOrInternalPolicyRepository.getExactlyOne(id);
    }

    public Country getCountryById(Long id) { return countryRepository.getExactlyOne(id); }

    public ListCategory getCategoryById(Long id) { return listCategoryRepository.getExactlyOne(id); }

    public Issuer getIssuerById(Long id) { return issuerRepository.getExactlyOne(id); }

    public SanctionType getSanctionTypeById(Long id) { return sanctionTypeRepository.getExactlyOne(id); }

    public ProgramSanctionType getProgramTypeById(long id) { return programSanctionTypeRepository.getExactlyOne(id); }

    public IdentityDocumentType getIdentityDocumentTypeById(Long id) {
        return identityDocumentTypeRepository.getExactlyOne(id);
    }

    public GeographicData getGeographicDataById(Long id) { return geographicDataRepository.getExactlyOne(id); }

    public RelationType getRelationTypeById(Long id) { return relationTypeRepository.getExactlyOne(id); }

    public <T extends Enum<?>> String translateEnum(T enumValue) {
        return translateEnum(enumValue, requestContext.getLocale());
    }

    public <T extends Enum<?>> String translateEnum(T enumValue, Locale locale) {
        return messageSource.getMessage(messageKeyForEnumValue(enumValue), null, locale);
    }

    public  String translateValue(String value) {
        return messageSource.getMessage(value, null, requestContext.getLocale());
    }

    public DateType getDateTypeById(Long id) { return dateTypeRepository.getExactlyOne(id); }

    public CountryType getCountryTypeById(Long id) { return countryTypeRepository.getExactlyOne(id); }

    public AliasType getAliasTypeById(Long id) { return aliasTypeRepository.getExactlyOne(id); }

    public AliasQuality getAliasQualityById(Long id) { return aliasQualityRepository.getExactlyOne(id); }

    public RecordCategory getRecordCategory(long id) { return recordCategoryRepository.getExactlyOne(id); }

    private <T extends Enum<?>> String messageKeyForEnumValue(T enumValue) {
        Class<?> enumClass = enumValue.getDeclaringClass();
        String enumName = enumClass.getSimpleName();
        if (enumValue.getDeclaringClass().getEnclosingClass() != null) {
            enumName = enumValue.getDeclaringClass().getEnclosingClass().getSimpleName() + "." + enumName;
        }
        return enumName + "." + enumValue.toString();
    }

    public <T extends Enum<?>> Comparator<T> localizedLabelComparator(Class<T> enumClass) {
        TreeSet<String> labels = new TreeSet<>();
        for (T enumValue : enumClass.getEnumConstants()) {
            labels.add(translateEnum(enumValue));
        }
        final Map<T, Integer> orderForEnumValue = new HashMap<>();
        int index = 0;
        for (String label : labels) {
            for (T enumValue : enumClass.getEnumConstants()) {
                if (label.equals(translateEnum(enumValue))) {
                    orderForEnumValue.put(enumValue, index);
                }
            }
            index++;
        }
        return Comparator.comparingInt(orderForEnumValue::get);
    }
}
