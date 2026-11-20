package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.importer.manual.RecordFieldIndexes;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class RecordValidatorWeak implements RecordValidator {

    private Map<AuditedFieldCode, Function<String, Object>> customFieldMappers;
    private RecordFieldIndexes indexes;

    public RecordValidatorWeak() {
    }

    @Override
    public List<ValidationError> validateRecord(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateFieldsIntegrity(mapping, row));
        errors.addAll(validateFieldsPatterns(mapping, row));
        return errors;
    }

    private List<ValidationError> validateFieldsIntegrity(MappingFile mapping, List<String> row) {
        List<AuditedFieldCode> requiredColumns = FieldIntegrityValidatorWeak.getAllPossibleRequiredColumns();
        return FieldIntegrityValidatorWeak.validateMinimumRequiredFieldsArePresentForImport(
                mapping.getColumns(), requiredColumns);
    }

    private List<ValidationError> validateFieldsPatterns(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        // STEP_A
        errors.addAll(validateFieldValues(mapping, row));
        return errors;
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
}
