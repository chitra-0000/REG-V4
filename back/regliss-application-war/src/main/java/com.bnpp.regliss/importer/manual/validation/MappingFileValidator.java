package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-10 of the original file were not visible in the photos]

public interface MappingFileValidator {
    String STRONG_PROFILE = "manualImportValidationStrong";
    List<ValidationError> validate(MappingFile mapping);

    RecordValidator createRecordValidator(Map<AuditedFieldCode, Function<String, Object>> customFieldMappers, RecordFieldIndexes indexes);
}

