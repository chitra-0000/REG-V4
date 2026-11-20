package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-19 of the original file were not visible in the photos]

public class FieldIntegrityValidatorWeak {

    private static final List<AuditedFieldCode> PROGRAM_FIELDS = asList(RECORD_PROGRAM_LABEL,   RECORD_PROGRAM_DESCRIPTION,
            RECORD_PROGRAM_SANCTION_TYPE);



    private FieldIntegrityValidatorWeak() { throw new IllegalStateException("Utility class"); }

    public static List<MappingColumn> getAllPossibleRequiredColumns(MappingFile mapping) {
        List<MappingColumn> candidateColumns = new ArrayList<>();
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_TYPE));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_NAME));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.PERSON_LAST_NAME));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_EXTERNAL_REFERENCE));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_PROGRAM_LABEL));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_PROGRAM_DESCRIPTION));
        candidateColumns.addAll(mapping.getColumnsForField(AuditedFieldCode.RECORD_PROGRAM_SANCTION_TYPE));

        return candidateColumns;
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresentForImport(MappingFile mapping, List<String> row){
        List<ValidationError> errors = new ArrayList<>();
         List<MappingColumn> candidateColumns = FieldIntegrityValidatorWeak.getAllPossibleRequiredColumns(mapping);
        List<AuditedFieldCode> actualFields = candidateColumns.stream()
                .filter(col -> StringUtils.isNotEmpty(row.get(col.getColumnIndex())))
                .map(MappingColumn::getFieldCode)
                .collect(toList());
        errors.addAll(validateMinimumGeneralFieldsArePresent(actualFields));
        errors.addAll(validateProgramsForImport(mapping, row));

        return errors;
    }

    public static List<ValidationError> validateMinimumRequiredFieldsArePresentForMapping(List<MappingColumn> columns){
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> fields = columns.stream().map(MappingColumn::getFieldCode).collect(Collectors.toList());
        errors.addAll(validateMinimumGeneralFieldsArePresent(fields));
        errors.addAll(validateProgramsForMapping(columns));

        return errors;
    }

    private static List<ValidationError> validateProgramsForImport(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> actualFields = mapping.getColumns().stream()
                .filter(col -> StringUtils.isNotEmpty(row.get(col.getColumnIndex())))
                .map(MappingColumn::getFieldCode)
                .collect(toList());
        errors.addAll(validateAtLeastOneProgram(actualFields));
        if(errors.isEmpty()) {
            errors.addAll(validateProgramsIntegrityForImport(mapping, row));
        }
        return errors;
    }

    private static List<ValidationError> validateProgramsForMapping(List<MappingColumn> columns) {
        List<ValidationError> errors = new ArrayList<>();
        List<AuditedFieldCode> fields = columns.stream().map(MappingColumn::getFieldCode).collect(Collectors.toList());
        errors.addAll(validateAtLeastOneProgram(fields));
        if(errors.isEmpty()) {
            errors.addAll(validateProgramsIntegrityForMapping(columns));
        }
        return errors;
    }

    private static List<ValidationError> validateProgramsIntegrityForMapping(List<MappingColumn> columns) {
        Map<Integer, List<MappingColumn>> allProgramsColumns = columns.stream()
                .filter(c -> PROGRAM_FIELDS.contains(c.getFieldCode()))
                .collect(groupingBy(MappingColumn::getChildIndex));

        if(allProgramsColumns.isEmpty()) {
            return emptyList(); // Avoid reporting error twice - case is treated before
        }
        List<ValidationError> errors = new ArrayList<>();
        for (Entry<Integer, List<MappingColumn>> aProgramColumns : allProgramsColumns.entrySet()) {
             List<AuditedFieldCode> actualProgramColumns = aProgramColumns.getValue().stream()
                    .map(MappingColumn::getFieldCode).collect(toList());
            errors.addAll(validateProgramLabel(actualProgramColumns, aProgramColumns.getKey()));
        }
        return errors;
    }

    private static List<ValidationError> validateProgramsIntegrityForImport(MappingFile mapping, List<String> row) {
        List<List<MappingColumn>> allProgramsColumns = mapping.getAllChildrenColumns().stream()
                .filter(c -> c.get(0).getFieldCode().getCategory() == SemiAutoImportFieldCategory.PROGRAM)
                .collect(toList());
        boolean recordHasNoActualProgram = allProgramsColumns.stream().flatMap(List::stream)
                .noneMatch(col -> isNotEmpty(row.get(col.getColumnIndex())));
        if(recordHasNoActualProgram) {
            return emptyList();  // Avoid reporting error twice - case is treated before
        }
        List<ValidationError> errors = new ArrayList<>();
        for (List<MappingColumn> aProgramColumns : allProgramsColumns) {
             List<AuditedFieldCode> actualProgramColumns = aProgramColumns.stream()
                    .filter(col -> isNotEmpty(row.get(col.getColumnIndex())))
                    .map(MappingColumn::getFieldCode).collect(toList());
             if(!actualProgramColumns.isEmpty()) {
                 errors.addAll(validateProgramLabel(actualProgramColumns, aProgramColumns.get(0).getChildIndex()));
             }
        }
        return errors;
    }

    private static List<ValidationError> validateMinimumGeneralFieldsArePresent(List<AuditedFieldCode> fieldCodes) {
        List<ValidationError> errors = new ArrayList<>();
        if (!fieldCodes.contains(AuditedFieldCode.RECORD_TYPE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_TYPE));
        }
        if (!fieldCodes.contains(AuditedFieldCode.RECORD_NAME) && !fieldCodes.contains(AuditedFieldCode.PERSON_LAST_NAME)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_NAME));
        }

        if (!fieldCodes.contains(AuditedFieldCode.RECORD_EXTERNAL_REFERENCE)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_RECORD_MISSING_EXTERNAL_REFERENCE));
        }
        return errors;
    }

    private static List<ValidationError> validateAtLeastOneProgram(List<AuditedFieldCode> fields) {
        Map<AuditedFieldCode, Long> countersByProgramFields = fields.stream()
                .filter(PROGRAM_FIELDS::contains)
                .collect(groupingBy(f -> f, counting()));

        Optional<Long> maxCountOfAProgramField = countersByProgramFields.values().stream().max(Long::compare);

        if (!maxCountOfAProgramField.isPresent()) {
            return Collections.singletonList(new ValidationError(ErrorCode.IMPORT_RECORD_WITHOUT_PROGRAMS));
        }
        return Collections.emptyList();
    }

    private static List<ValidationError> validateProgramLabel(List<AuditedFieldCode> actualProgramColumns, Integer childIndex) {
        List<ValidationError> errors = new ArrayList<>();
        if (!actualProgramColumns.contains(AuditedFieldCode.RECORD_PROGRAM_LABEL)) {
            errors.add(new ValidationError(ErrorCode.IMPORT_PROGRAM_WITHOUT_LABEL, childIndex));
        }
        return errors;
    }

}
