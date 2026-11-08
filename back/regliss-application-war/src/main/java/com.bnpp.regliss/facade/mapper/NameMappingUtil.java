package com.bnpp.regliss.facade.mapper;

import com.bnpp.regliss.entity.record.PersonRecord;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordType;
import org.apache.commons.lang.StringUtils;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class NameMappingUtil {
    private NameMappingUtil(){
    }
    public static  String toUppercaseLastName(String fullName, Record record){
        if( record != null && RecordType.PERSON.equals(record.getType())) {
            PersonRecord person = (PersonRecord)record;
            if(StringUtils.isNotEmpty(person.getLastName())){
                return collectNameFromParts(person.getFirstName(),person.getMiddleName(),person.getLastName().toUpperCase(),person.getSuffix());
            }
            if(StringUtils.isEmpty(record.getName()) &&
                    (StringUtils.isNotEmpty(person.getFirstName())||StringUtils.isNotEmpty(person.getMiddleName()))){
                return collectNameFromParts(person.getFirstName(),person.getMiddleName(),person.getSuffix());
            }
        }
        return fullName;
    }

    private static String collectNameFromParts(String... parts) {
        return Stream.of(parts).filter(StringUtils::isNotEmpty).collect(joining(" "));
    }
}
