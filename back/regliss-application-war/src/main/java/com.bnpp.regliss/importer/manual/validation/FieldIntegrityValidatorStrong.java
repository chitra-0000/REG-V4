package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

public class FieldIntegrityValidatorStrong {

    private static final List<SemiAutoImportFieldCategory> RECORD_REQUIRED_FIELD_CATEGORIES =
            asList(
                    SemiAutoImportFieldCategory.GENERAL_INFORMATION,
                    SemiAutoImportFieldCategory.PERSON,
                    SemiAutoImportFieldCategory.ENTITY,
                    SemiAutoImportFieldCategory.VESSEL,
                    SemiAutoImportFieldCategory.OTHER,
                    SemiAutoImportFieldCategory.CITY,
                    SemiAutoImportFieldCategory.COUNTRY,
                    SemiAutoImportFieldCategory.AIRPORT,
                    SemiAutoImportFieldCategory.SEAPORT,
                    SemiAutoImportFieldCategory.PROGRAM,
                    SemiAutoImportFieldCategory.SANCTION_TYPE);

    private FieldIntegrityValidatorStrong() { throw new IllegalStateException("Utility class"); }

    public static List<MappingColumn> getAllPossibleRequiredColumns(MappingFile mapping) {
        return mapping.getAllFieldsOfCategories(RECORD_REQUIRED_FIELD_CATEGORIES);
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresent(Set<AuditedFieldCode> fields) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateMinimumGeneralFieldsArePresent(fields));
        errors.addAll(validateAtLeastOneProgram(fields));
        return errors;
    }

    private static List<ValidationError> validateMinimumGeneralFieldsArePresent(Set<AuditedFieldCode> fields) {
        List<ValidationError> errors = new ArrayList<>();
        if (!fields.contains(AuditedFieldCode.RECORD_TYPE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_TYPE, AuditedFieldCode.RECORD_TYPE.name()));
        }
        if (!fields.contains(AuditedFieldCode.RECORD_NAME) && !fields.contains(AuditedFieldCode.PERSON_LAST_NAME)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_NAME, AuditedFieldCode.RECORD_NAME.name()));
        }

        if (!fields.contains(AuditedFieldCode.RECORD_EXTERNAL_REFERENCE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_EXTERNAL_REFERENCE,
                    AuditedFieldCode.RECORD_EXTERNAL_REFERENCE.name()));
        }

        if (!fields.contains(AuditedFieldCode.RECORD_START_DATE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_START_DATE, AuditedFieldCode.RECORD_START_DATE.name()));
        }

        if (!fields.contains(AuditedFieldCode.RECORD_SANCTION_TYPE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_SANCTION_TYPE, AuditedFieldCode.RECORD_SANCTION_TYPE.name()));
        }

        if (!fields.contains(AuditedFieldCode.RECORD_DOW_JONES_NOTES)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_NOTES, AuditedFieldCode.RECORD_DOW_JONES_NOTES.name()));
        }
        return errors;
    }

    private static List<ValidationError> validateAtLeastOneProgram(Set<AuditedFieldCode> fields) {
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> programFields = Arrays.asList(AuditedFieldCode.RECORD_PROGRAM_LABEL,
                AuditedFieldCode.RECORD_PROGRAM_DESCRIPTION, AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE);

        Map<AuditedFieldCode, Long> countersByProgramFields = fields.stream()
                .filter(programFields::contains)
                .collect(groupingBy(f -> f, Collectors.counting()));

        Optional<Long> maxCountOfAProgramField = countersByProgramFields.values().stream().max(Long::compare);

        if (!maxCountOfAProgramField.isPresent()) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_WITHOUT_PROGRAMS, AuditedFieldCode.RECORD_PROGRAM_LABEL.name()));
        } else {
            Long programLabelCount = countersByProgramFields.get(AuditedFieldCode.RECORD_PROGRAM_LABEL);
            if (programLabelCount != null && programLabelCount < maxCountOfAProgramField.get()) {
                errors.add(new ValidationError(ErrorCode.IMPORT_PROGRAM_WITHOUT_LABEL, AuditedFieldCode.RECORD_PROGRAM_LABEL.name()));
            }
        }
        return errors;
    }

    public static List<ValidationError> validateProgramLabel(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!childFields.contains(AuditedFieldCode.RECORD_PROGRAM_LABEL)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_PROGRAM_WITHOUT_LABEL, childIndex));
        }
        return errors;
    }

    public static List<ValidationError> validateAddress(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!childFields.contains(AuditedFieldCode.RECORD_ADDRESS_CITY) &&
            !childFields.contains(AuditedFieldCode.RECORD_ADDRESS_COUNTRY)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_ADDRESS_WITHOUT_CITY_COUNTRY, childIndex));
        }

        if (childFields.contains(AuditedFieldCode.RECORD_ADDRESS_ADDRESS) &&
            !childFields.contains(AuditedFieldCode.RECORD_ADDRESS_CITY)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_ADDRESS_STREET_WITHOUT_CITY, childIndex));
        }
        return errors;
    }

    public static List<ValidationError> validateAlias(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!childFields.contains(AuditedFieldCode.RECORD_ALIAS_EXTERNAL_REFERENCE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_ALIAS_WITHOUT_REFERENCE, childIndex));
        }

        if (!childFields.contains(AuditedFieldCode.RECORD_ALIAS_LAST_NAME)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_ALIAS_WITHOUT_NAME, childIndex));
        }
        return errors;
    }

    public static List<ValidationError> validateIdentityDocument(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!childFields.contains(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_DOCUMENT_WITHOUT_TYPE, childIndex));
        }

        if (!childFields.contains(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_NUMBER)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_DOCUMENT_WITHOUT_NUMBER, childIndex));
        }
        return errors;
    }

    public static List<ValidationError> validateBiography(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> minimumFields = new ArrayList<>(Arrays.asList(
                AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_BIRTH_PLACE,
                AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_DATE_VALUE,
                AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_BIRTH_COUNTRY));
        minimumFields.retainAll(childFields);
        if (minimumFields.isEmpty()) {
            errors.add(new ValidationError(ErrorCode.IMPORT_BIOGRAPHY_EMPTY, childIndex));
        }
        return errors;
    }

    public static List<ValidationError> validateNationality(Set<AuditedFieldCode> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!childFields.contains(AuditedFieldCode.RECORD_COUNTRY_DETAILS_BIRTH_COUNTRY)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_NATIONALITY_WITHOUT_COUNTRY, childIndex));
        }
        return errors;
    }

}
