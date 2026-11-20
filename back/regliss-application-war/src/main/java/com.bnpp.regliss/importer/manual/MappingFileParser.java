package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.ReglissException.ErrorCode;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.annotation.AuditedFieldCode.SemiAutoImportFieldCategory;
import com.bnpp.regliss.entity.MappingColumn;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.importer.manual.validation.MappingFileValidator;
import com.bnpp.regliss.service.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

@Service
public class MappingFileParser {

    private static final List<AuditedFieldCode> ALLOWED_DUPLICATED_FIELDS = Arrays.asList(
            AuditedFieldCode.RECORD_COMMENTS,
            AuditedFieldCode.RECORD_ADDITIONAL_INFO);

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private MappingFileValidator mappingFileValidator;

    public void setMappingFileValidator(MappingFileValidator mappingFileValidator) {
        this.mappingFileValidator = mappingFileValidator;
    }

    public void parseAndValidateFile(MappingFile mappingFile) {
        identifyAndSetChildrenIndexes(mappingFile);
        validateNoDuplicateFields(mappingFile);
        validateRequiredFields(mappingFile);
    }

    private void validateNoDuplicateFields(MappingFile mappingFile) {
        Map<SemiAutoImportFieldCategory, List<MappingColumn>> groupedFieldsMap = groupFieldsByCategory(mappingFile);
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.GENERAL_INFORMATION,
                groupedFieldsMap.get(SemiAutoImportFieldCategory.GENERAL_INFORMATION));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.PERSON, groupedFieldsMap.get(SemiAutoImportFieldCategory.PERSON));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.ENTITY, groupedFieldsMap.get(SemiAutoImportFieldCategory.ENTITY));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.VESSEL, groupedFieldsMap.get(SemiAutoImportFieldCategory.VESSEL));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.AIRPORT, groupedFieldsMap.get(SemiAutoImportFieldCategory.AIRPORT));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.SEAPORT, groupedFieldsMap.get(SemiAutoImportFieldCategory.SEAPORT));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.CITY, groupedFieldsMap.get(SemiAutoImportFieldCategory.CITY));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.COUNTRY, groupedFieldsMap.get(SemiAutoImportFieldCategory.COUNTRY));
        validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory.OTHER, groupedFieldsMap.get(SemiAutoImportFieldCategory.OTHER));
    }

    private void validateNoDuplicateFieldsForCategory(SemiAutoImportFieldCategory fieldCategory, List<MappingColumn> fields) {
        Map<AuditedFieldCode, Long> fieldToOccurrenceCount = fields.stream()
                .collect(groupingBy(MappingColumn::getFieldCode, counting()));
        List<AuditedFieldCode> repeatingFields = fieldToOccurrenceCount.entrySet().stream()
                .filter(e -> !ALLOWED_DUPLICATED_FIELDS.contains(e.getKey()))
                .filter(e -> e.getValue() >= 2)
                .map(Entry::getKey)
                .collect(toList());
        if (!repeatingFields.isEmpty()) {
            throw new ReglissException(
                    ErrorCode.MAPPING_FILE_DUPLICATE_FIELDS,
                    referenceService.translateEnum(fieldCategory),
                    repeatingFields.stream().map(referenceService::translateEnum).collect(toList()).toString());
        }
    }

    private void validateRequiredFields(MappingFile mappingFile) {
        List<ValidationError> errors = mappingFileValidator.validate(mappingFile);

        if (!errors.isEmpty()) {
            ValidationError firstError = errors.get(0);
            throw new ReglissException(firstError.getErrorCode(), firstError.getParameters());
        }
    }

    private void identifyAndSetChildrenIndexes(MappingFile mapping) {
        Map<SemiAutoImportFieldCategory, List<List<MappingColumn>>> childrenColumns = Stream
                .of(SemiAutoImportFieldCategory.values()).collect(toMap(Function.identity(), l -> new ArrayList<>()));

        Map<SemiAutoImportFieldCategory, List<MappingColumn>> categoriesBucket = Stream
                .of(SemiAutoImportFieldCategory.values()).collect(toMap(Function.identity(), l -> new ArrayList<>()));

        for (MappingColumn field : mapping.getColumns()) {
            SemiAutoImportFieldCategory childCategory = field.getFieldCode().getCategory();

            List<MappingColumn> categoryBucket = categoriesBucket.get(childCategory);

            if (categoryBucket == null) {
                throw new ReglissException("Field " + field.getFieldCode() + " has category " + childCategory);
            }
            if (columnAlreadyInBucket(field, categoryBucket)) {
                List<List<MappingColumn>> childrenForCategory = childrenColumns.get(childCategory);
                childrenForCategory.add(new ArrayList<>(categoryBucket));
                childrenColumns.put(childCategory, childrenForCategory);
                categoryBucket = new ArrayList<>();
            }

            field.setChildIndex(childrenColumns.get(childCategory).size());
            categoryBucket.add(field);
            categoriesBucket.put(childCategory, categoryBucket);
        }
    }

    private boolean columnAlreadyInBucket(MappingColumn field, List<MappingColumn> bucket) {
        return bucket.stream().anyMatch(oldField -> oldField.getFieldCode() == field.getFieldCode());
    }

    private Map<SemiAutoImportFieldCategory, List<MappingColumn>> groupFieldsByCategory(MappingFile mappingFile) {
        Map<SemiAutoImportFieldCategory, List<MappingColumn>> groupedFieldsMap = new EnumMap<>(SemiAutoImportFieldCategory.class);

        for (SemiAutoImportFieldCategory category : SemiAutoImportFieldCategory.values()) {
            List<MappingColumn> fieldsOfThisCategory = mappingFile.getColumns().stream()
                    .filter(column -> column.getFieldCode().getCategory() == category)
                    .collect(toList());
            groupedFieldsMap.put(category, fieldsOfThisCategory);
        }
        return groupedFieldsMap;
    }


}

