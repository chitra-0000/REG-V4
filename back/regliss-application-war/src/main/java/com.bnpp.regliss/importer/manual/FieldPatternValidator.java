package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-21 of the original file were not visible in the photos]

public class FieldPatternValidator {

    private  FieldPatternValidator(){}
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private static boolean invalidDate(String s) {
        if (s != null) {
            try {
                LocalDate.parse(s, ofPattern(DATE_PATTERN));
            } catch (DateTimeParseException e) {
                return true;
            }
        }
        return false;
    }

    public static List<ValidationError> validateAttributesPatterns(Function<AuditedFieldCode, String> fields, Map<AuditedFieldCode,
            Function<String, Object>> customFieldMappers) {
        List<ValidationError> errors = new ArrayList<>();
        if (invalidDate(fields.apply(AuditedFieldCode.RECORD_START_DATE))) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_RECORD_INVALID_START_DATE_FORMAT, DATE_PATTERN));
        }
        errors.addAll(validateFullNameLength(fields));
        errors.addAll(validateNamesForPersonRecord(fields, customFieldMappers));
        return errors;
    }

    private static List<ValidationError> validateNamesForPersonRecord(Function<AuditedFieldCode, String> fields, Map<AuditedFieldCode,
            Function<String, Object>> customFieldMappers) {
        List<ValidationError> errors = new ArrayList<>();
        String recordType = fields.apply(AuditedFieldCode.RECORD_TYPE);
        if (recordType == null) {
            return errors;
        }
        if (isIndividual(recordType, customFieldMappers.get(AuditedFieldCode.RECORD_TYPE))) {
            if (fields.apply(AuditedFieldCode.PERSON_LAST_NAME) == null) {
                errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_PERSON_MISSING_LAST_NAME));
            }
            if (fields.apply(AuditedFieldCode.RECORD_NAME)!=null) {
                errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_PERSON_RECORD_NAME_EXISTS,
                        AuditedFieldCode.RECORD_NAME.name()));
            }
        }
        return errors;
    }

    private static boolean isIndividual(String recordType, Function<String, Object> function) {
        try {
            RecordType resolvedRecordType = (RecordType) function.apply(recordType);
            return RecordType.PERSON.equals(resolvedRecordType);
        }catch(ReglissException e) {
            return false;
        }
    }

    private static List<ValidationError> validateFullNameLength(Function<AuditedFieldCode, String> fields) {
        List<ValidationError> errors = new ArrayList<>();
        if (fields.apply(AuditedFieldCode.RECORD_TYPE) == null) {
            return emptyList(); // will cause errors in other places anyway
        }
        if (fields.apply(AuditedFieldCode.RECORD_TYPE).equals(RecordType.PERSON.toString())) {
            String[] names = {
                    fields.apply(AuditedFieldCode.PERSON_LAST_NAME),
                    fields.apply(AuditedFieldCode.PERSON_FIRST_NAME),
                    fields.apply(AuditedFieldCode.PERSON_MIDDLE_NAME)};
            String fullName = Stream.of(names)
                    .filter(StringUtils::isNotEmpty).collect(joining(" "));
            if (fullName.length() < 3) {
                errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_PERSON_FULL_NAME_MIN_LENGTH));
            }
        } else if (fields.apply(AuditedFieldCode.RECORD_NAME)==null &&
                fields.apply(AuditedFieldCode.PERSON_LAST_NAME) != null) {
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_RECORD_MISSING_NAME, AuditedFieldCode.RECORD_NAME.name()));
        }
        return errors;
    }
    public static List<ValidationError> validateIdentityDocumentPatterns(Function<AuditedFieldCode,
            String> childFields, Integer childIndex, Function<String, ?> identityDocumentTypeResolver) {
        List<ValidationError> errors = new ArrayList<>();
        String documentType = childFields.apply(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE);
        String documentNumber = childFields.apply(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_NUMBER);

        if (documentType != null && documentNumber != null)  {
            IdentityDocumentType type = (IdentityDocumentType) identityDocumentTypeResolver.apply(documentType);
            if (type != null && type.hasBusinessConcept(ReferentialBusinessConcept.IDT_PASSPORT)
                    && documentNumber.trim().length() < 4) {
                    errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_PASSPORT_WITH_INVALID_NUMBER, childIndex));
            }
        }
        if(invalidDate(childFields.apply(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_ISSUING_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_INVALID_DOCUMENT_ISSUING_DATE_FORMAT,
                    childIndex, DATE_PATTERN));
// [MISSING FROM SCREENSHOTS: lines 115-115 of the original file were not visible in the photos]

        if(invalidDate(childFields.apply(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_EXPIRATION_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_INVALID_DOCUMENT_EXPIRATION_DATE_FORMAT,
                    childIndex, DATE_PATTERN));
        }
        return errors;
    }

    public static List<ValidationError> validateSanctionReferencePatterns(Function<AuditedFieldCode,
            String> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if(invalidDate(childFields.apply(AuditedFieldCode.RECORD_SANCTION_REFERENCE_SINCE_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_INVALID_SANCTION_REFERENCE_SINCE_DATE,
                    childIndex, DATE_PATTERN));
        }

        if(invalidDate(childFields.apply(AuditedFieldCode.RECORD_SANCTION_REFERENCE_END_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_INVALID_SANCTION_REFERENCE_END_DATE,
                    childIndex, DATE_PATTERN));
        }
        return errors;
    }

    public static List<ValidationError> validateDateDetailsPattern(Function<AuditedFieldCode, String> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if(invalidDate(childFields.apply(AuditedFieldCode.RECORD_PERSON_BIOGRAPHY_DATE_VALUE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_RECORD_INVALID_DATE_DETAILS_VALUE_FORMAT,
                    childIndex, DATE_PATTERN));
        }

        return errors;
    }

    public static List<ValidationError> validateFunctionPatterns(Function<AuditedFieldCode, String> childFields, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if(invalidDate(childFields.apply(AuditedFieldCode.PERSON_FUNCTION_FROM_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_RECORD_INVALID_FUNCTION_FROM_DATE, childIndex, DATE_PATTERN));
        }

        if(invalidDate(childFields.apply(AuditedFieldCode.PERSON_FUNCTION_TO_DATE))){
            errors.add(new ValidationError(ReglissException.ErrorCode.IMPORT_RECORD_INVALID_FUNCTION_TO_DATE, childIndex, DATE_PATTERN));
        }
        return errors;
    }
}

