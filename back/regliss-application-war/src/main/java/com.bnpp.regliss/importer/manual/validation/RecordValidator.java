package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public interface RecordValidator {
    List<ValidationError> validateRecord(MappingFile mapping, List<String> row);
}

