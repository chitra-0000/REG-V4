package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.MappingColumn;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.ReglissException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class FieldIntegrityValidatorStrong {

    private FieldIntegrityValidatorStrong() {
    }

    private static final List<AuditedFieldCode.ChildCategory> RECORD_REQUIRED_FIELD_CATEGORIES = Arrays.asList(
            AuditedFieldCode.ChildCategory.GENERAL_INFORMATION,
            AuditedFieldCode.ChildCategory.PERSON,
            AuditedFieldCode.ChildCategory.ENTITY,
            AuditedFieldCode.ChildCategory.VESSEL,
            AuditedFieldCode.ChildCategory.OTHER,
            AuditedFieldCode.ChildCategory.CITY,
            AuditedFieldCode.ChildCategory.COUNTRY,
            AuditedFieldCode.ChildCategory.AIRPORT,
            AuditedFieldCode.ChildCategory.SEAPORT,
            AuditedFieldCode.ChildCategory.PROGRAM,
            AuditedFieldCode.ChildCategory.SANCTION_TYPE
    );

    public static List<AuditedFieldCode> getAllPossibleRequiredColumns() {
        return Arrays.stream(AuditedFieldCode.values())
                .filter(f -> RECORD_REQUIRED_FIELD_CATEGORIES.contains(f.getChildCategory()))
                .collect(toList());
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresent(
            List<MappingColumn> columns, List<AuditedFieldCode> allPossibleRequiredColumns) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateMinimumGeneralFieldsArePresent(columns));
        errors.addAll(validateAtLeastOneProgram(columns));
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
        return errors;
    }

    private static List<ValidationError> validateAtLeastOneProgram(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> programFields = Arrays.asList(
                AuditedFieldCode.RECORD_PROGRAM_LABEL,
                AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE
        );
        Map<AuditedFieldCode, Long> countersByProgramFields = columns.stream()
                .filter(c -> programFields.contains(c.getFieldCode()))
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

    public static List<ValidationError> validateProgramLabel(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        boolean hasProgramLabel = columns.stream()
                .anyMatch(c -> c.getFieldCode() == AuditedFieldCode.RECORD_PROGRAM_LABEL);
        if (!hasProgramLabel) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_MISSING_PROGRAM_LABEL));
        }
        return errors;
    }

    public static List<ValidationError> validateAddress(List<MappingColumn> columns) {
        // TODO: [GAP] - address validation logic not fully visible in photos
        return new ArrayList<>();
    }

    public static List<ValidationError> validateAlias(List<MappingColumn> columns) {
        // TODO: [GAP] - alias validation logic not fully visible in photos
        return new ArrayList<>();
    }

    public static List<ValidationError> validateIdentityDocument(List<MappingColumn> columns) {
        // TODO: [GAP] - identity document validation for mapping columns
        return new ArrayList<>();
    }

    public static List<ValidationError> validateBiography(List<MappingColumn> columns) {
        // TODO: [GAP] - biography validation logic not fully visible in photos
        return new ArrayList<>();
    }

    public static List<ValidationError> validateNationality(List<MappingColumn> columns) {
        // TODO: [GAP] - nationality validation logic not fully visible in photos
        return new ArrayList<>();
    }
}
