package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.MappingColumn;
import com.bnpp.regliss.importer.manual.FieldPatternValidator;
import com.bnpp.regliss.importer.manual.RecordFieldIndexes;
import com.bnpp.regliss.utils.PerformanceMonitor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class RecordValidatorStrong implements RecordValidator {

    private Map<AuditedFieldCode, Function<String, Object>> customFieldMappers;
    private RecordFieldIndexes indexes;

    public RecordValidatorStrong() {
    }

    @Override
    public List<ValidationError> validateRecord(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateFieldsIntegrity(mapping, row));
        errors.addAll(validateFieldsPatterns(mapping, row));
        return errors;
    }

    private List<ValidationError> validateFieldsIntegrity(MappingFile mapping, List<String> row) {
        List<AuditedFieldCode> requiredColumns = FieldIntegrityValidatorStrong.getAllPossibleRequiredColumns();
        List<MappingColumn> presentColumns = mapping.getColumns().stream()
                .filter(c -> columnIsPresent(c, row))
                .collect(java.util.stream.Collectors.toList());
        List<ValidationError> errors = FieldIntegrityValidatorStrong.validateMinimumRequiredFieldsArePresent(
                presentColumns, requiredColumns);
        return decorateErrorsWithColumnNamesInRow(errors, mapping);
    }

    private boolean columnIsPresent(MappingColumn column, List<String> row) {
        String value = row.get(column.getColumnIndex());
        // TODO: also checks resolver - exact logic partially visible
        return value != null;
    }

    private List<ValidationError> validateFieldsPatterns(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        // STEP_A
        PerformanceMonitor.start("STEP_A");
        errors.addAll(validateFieldValues(mapping, row));
        PerformanceMonitor.stop("STEP_A");
        // STEP_B
        PerformanceMonitor.start("STEP_B");
        Function<AuditedFieldCode, String> fieldAccessor = fieldCode -> indexes.getSingleColumnValue(fieldCode, row);
        errors.addAll(FieldPatternValidator.validateAttributesPatterns(fieldAccessor, customFieldMappers));
        PerformanceMonitor.stop("STEP_B");
        // STEP_C
        PerformanceMonitor.start("STEP_C");
        for (Map.Entry<Integer, AuditedFieldCode.ChildCategory> childEntry : mapping.getChildrenIndexes().entrySet()) {
            Integer childIndex = childEntry.getKey();
            AuditedFieldCode.ChildCategory childCategory = childEntry.getValue();
            Function<AuditedFieldCode, String> childFields = fieldCode -> indexes.getChildColumnValue(fieldCode, childIndex, row);
            errors.addAll(validateChildPattern(childCategory, childFields, childIndex));
        }
        PerformanceMonitor.stop("STEP_C");
        return errors;
    }

    private List<ValidationError> validateChildPattern(AuditedFieldCode.ChildCategory childCategory,
                                                       Function<AuditedFieldCode, String> childFields,
                                                       Integer childIndex) {
        switch (childCategory) {
            case IDENTITY:
                return FieldPatternValidator.validateIdentityDocumentPatterns(childFields, childIndex,
                        customFieldMappers.get(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE));
            case PERSON_BIOGRAPHY:
                return FieldPatternValidator.validateDateDetailsPattern(childFields, childIndex);
            case FUNCTION:
                return FieldPatternValidator.validateFunctionPatterns(childFields, childIndex);
            case SANCTION_REFERENCE:
                return FieldPatternValidator.validateSanctionReferencePatterns(childFields, childIndex);
            default:
                return new ArrayList<>();
        }
    }

    private List<ValidationError> validateFieldValues(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        for (Map.Entry<AuditedFieldCode, Function<String, Object>> entry : customFieldMappers.entrySet()) {
            AuditedFieldCode fieldCode = entry.getKey();
            Function<String, Object> fieldResolver = entry.getValue();
            List<String> columnValues = indexes.getAllColumnValues(fieldCode, row);
            for (String columnValue : columnValues) {
                if (columnValue != null) {
                    resolveFieldCollectingErrors(errors, fieldCode, fieldResolver, columnValue);
                }
            }
        }
        return errors;
    }

    private void resolveFieldCollectingErrors(List<ValidationError> errors, AuditedFieldCode fieldCode,
                                              Function<String, Object> fieldResolver, String columnValue) {
        try {
            fieldResolver.apply(columnValue);
        } catch (ReglissException e) {
            errors.add(new ValidationError(e.getErrorCode(), fieldCode.name(), columnValue));
        }
    }

    private List<ValidationError> decorateErrorsWithColumnNamesInRow(List<ValidationError> errors,
                                                                     MappingFile mapping) {
        return errors.stream()
                .map(error -> {
                    String columnLabel = determineColumnLabel(error, mapping);
                    // TODO: exact decoration logic partially visible
                    return error;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private String determineColumnLabel(ValidationError error, MappingFile mapping) {
        return mapping.getColumns().stream()
                .filter(c -> c.getFieldCode() == error.getFieldCode())
                .map(MappingColumn::getLabel)
                .findFirst()
                .orElse(null);
    }
}
