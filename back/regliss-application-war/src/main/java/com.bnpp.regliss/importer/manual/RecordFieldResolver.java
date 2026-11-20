package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import static com.bnpp.regliss.annotation.AuditedFieldCode.RECORD_ALIAS_QUALITY;

@Slf4j
@Service
@ReglissBatchProfile
public class RecordFieldResolver {

    @Autowired
    private ManualImportReferentialsExtractor referentialsExtractor;

    public Map<AuditedFieldCode, Function<String, Object>> getCustomFieldMapper(){
        Map<AuditedFieldCode, Function<String, Object>> customFieldMappers =  new EnumMap<>(AuditedFieldCode.class);
        Map<AuditedFieldCode, Map<String, Object>> allLabelMappings = referentialsExtractor.getManualImportKeysToValuesMap();
        for (Entry<AuditedFieldCode, Map<String, Object>> entry : allLabelMappings.entrySet()) {
            customFieldMappers.put(entry.getKey(), mapOrDefault(entry.getValue(), entry.getKey()));
        }
        return customFieldMappers;
    }

    private Function<String, Object> mapOrDefault(Map<String, Object> labelMappings, AuditedFieldCode fieldCode) {
        return s -> getObjetByFieldCode(labelMappings, fieldCode, s);
    }

    private Object getObjetByFieldCode(Map<String, Object> labelMappings, AuditedFieldCode fieldCode, String currentValue){

        if( fieldCode == RECORD_ALIAS_QUALITY ) {
            return getAliasQualityObject(labelMappings, currentValue);
        } else {
            return getGeneralObject(labelMappings, currentValue, fieldCode);
        }
    }

    private Object getAliasQualityObject(Map<String, Object> labelMappings, String currentValue){
        if (StringUtils.isEmpty(currentValue)) {
            return labelMappings.get(referentialsExtractor.getAliasQualityStrongLabel());
        }
        String key = labelMappings.keySet().stream()
                .filter(currentValue.trim()::equalsIgnoreCase)
                .findFirst()
                .orElse("");
        return getObjectOrDefaultValue(labelMappings, labelMappings.get(key), currentValue, RECORD_ALIAS_QUALITY);
    }

    private Object getGeneralObject(Map<String, Object> labelMappings, String currentValue, AuditedFieldCode fieldCode){
        if (StringUtils.isEmpty(currentValue)) {
            return null;
        }
        return getObjectOrDefaultValue(labelMappings, labelMappings.get(currentValue), currentValue, fieldCode);
    }

    private Object getObjectOrDefaultValue(Map<String, Object> labelMappings, Object object, String currentValue, AuditedFieldCode fieldCode){
        if (object == null) {
            log.debug("For field {}, value '{}' not found in the known referentials",fieldCode, currentValue);

            object = labelMappings.get(ReferentialBusinessConcept.DEFAULT_VALUE.name());

            if(object == null) {
                log.info("For field {}, default value {} not found in the known referentials {}",fieldCode, currentValue, labelMappings);
            }
        }
        return object;
    }

}

