package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.MappingColumn;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.ReglissException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class FieldIntegrityValidatorWeak {

    private FieldIntegrityValidatorWeak() {
    }

    private static final List<AuditedFieldCode> PROGRAM_FIELDS = Arrays.asList(
            AuditedFieldCode.RECORD_PROGRAM_LABEL,
            AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE
    );

    public static List<AuditedFieldCode> getAllPossibleRequiredColumns() {
        List<AuditedFieldCode> result = new ArrayList<>();
        result.add(AuditedFieldCode.RECORD_TYPE);
        result.add(AuditedFieldCode.RECORD_NAME);
        result.add(AuditedFieldCode.PERSON_LAST_NAME);
        result.add(AuditedFieldCode.RECORD_EXTERNAL_REFERENCE);
        result.addAll(PROGRAM_FIELDS);
        return result;
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresentForImport(
            List<MappingColumn> columns, List<AuditedFieldCode> allPossibleRequiredColumns) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateMinimumGeneralFieldsArePresent(columns));
        errors.addAll(validateAtLeastOneProgram(columns));
        errors.addAll(validateProgramsForImport(columns));
        return errors;
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresentForMapping(
            List<MappingColumn> columns, List<AuditedFieldCode> allPossibleRequiredColumns) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateMinimumGeneralFieldsArePresent(columns));
        errors.addAll(validateAtLeastOneProgram(columns));
        errors.addAll(validateProgramsForMapping(columns));
        return errors;
    }

    public static List<ValidationError> validateProgramsForImport(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateProgramLabel(columns));
        return errors;
    }

    public static List<ValidationError> validateProgramsForMapping(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateProgramLabel(columns));
        errors.addAll(validateProgramsIntegrityForMapping(columns));
        return errors;
    }

    public static List<ValidationError> validateProgramsIntegrityForMapping(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        Map<AuditedFieldCode, List<MappingColumn>> allProgramsColumns = columns.stream()
                .filter(c -> PROGRAM_FIELDS.contains(c.getFieldCode()))
                .collect(Collectors.groupingBy(MappingColumn::getFieldCode));

        boolean recordHasNoActualProgram = allProgramsColumns.isEmpty()
                || allProgramsColumns.values().stream().allMatch(List::isEmpty);
        if (recordHasNoActualProgram) {
            return errors;
        }

        // Check that all program fields have the same number of columns
        long[] counts = allProgramsColumns.values().stream()
                .mapToLong(List::size)
                .distinct()
                .toArray();

        if (counts.length > 1) {
            for (AuditedFieldCode field : PROGRAM_FIELDS) {
                List<MappingColumn> fieldColumns = allProgramsColumns.getOrDefault(field, Collections.emptyList());
                if (fieldColumns.isEmpty()) {
                    errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_PROGRAM_FIELD,
                            field.name()));
                }
            }
        }
        return errors;
    }

    private static List<ValidationError> validateMinimumGeneralFieldsArePresent(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        Set<AuditedFieldCode> presentFields = columns.stream()
                .map(MappingColumn::getFieldCode)
                .collect(Collectors.toSet());

        if (!presentFields.contains(AuditedFieldCode.RECORD_TYPE)) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_RECORD_TYPE));
        }
        if (!presentFields.contains(AuditedFieldCode.RECORD_NAME)
                && !presentFields.contains(AuditedFieldCode.PERSON_LAST_NAME)) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_RECORD_NAME));
        }
        if (!presentFields.contains(AuditedFieldCode.RECORD_EXTERNAL_REFERENCE)) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_EXTERNAL_REFERENCE));
        }
        return errors;
    }

    private static List<ValidationError> validateAtLeastOneProgram(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        Map<AuditedFieldCode, Long> countersByProgramFields = columns.stream()
                .filter(c -> PROGRAM_FIELDS.contains(c.getFieldCode()))
                .collect(Collectors.groupingBy(MappingColumn::getFieldCode, Collectors.counting()));

        long maxCount = countersByProgramFields.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);
        if (maxCount == 0) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_PROGRAM));
        }
        return errors;
    }

    private static List<ValidationError> validateProgramLabel(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        boolean hasProgramLabel = columns.stream()
                .anyMatch(c -> c.getFieldCode() == AuditedFieldCode.RECORD_PROGRAM_LABEL);
        if (!hasProgramLabel) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_PROGRAM_LABEL));
        }
        return errors;
    }
}
