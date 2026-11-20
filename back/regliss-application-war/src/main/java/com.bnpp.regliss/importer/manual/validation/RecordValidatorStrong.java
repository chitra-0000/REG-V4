package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-24 of the original file were not visible in the photos]

@Slf4j
public class RecordValidatorStrong implements RecordValidator {

    private final Map<AuditedFieldCode, Function<String, Object>> customFieldMappers;
    private final RecordFieldIndexes indexes;

    public RecordValidatorStrong(Map<AuditedFieldCode, Function<String, Object>> customFieldMappers, RecordFieldIndexes indexes) {
        this.customFieldMappers = customFieldMappers;
        this.indexes = indexes;
    }

    @Override
    public List<ValidationError> validateRecord(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateFieldsIntegrity(mapping, row));
        errors.addAll(validateFieldsPatterns(row, customFieldMappers));
        return errors;
    }

    private List<ValidationError> validateFieldsIntegrity(MappingFile mapping, List<String> row){
        List<MappingColumn> columnsWithRequiredFields = FieldIntegrityValidatorStrong.getAllPossibleRequiredColumns(mapping);
        Set<AuditedFieldCode> actualFields = columnsWithRequiredFields.stream()
                .filter(col -> columnIsPresent(row, col))
                .map(MappingColumn::getFieldCode)
                .collect(toSet());

        List<ValidationError> errors = FieldIntegrityValidatorStrong.validateMinimumRequiredFieldsArePresent(actualFields);
        log.trace("Fields integrity validation completed with {} errors", errors.size());
        return decorateErrorsWithColumnNamesInRow(errors, mapping);
    }

    private boolean columnIsPresent(List<String> row, MappingColumn col) {
        String valueInFile = row.get(col.getColumnIndex());
        if (StringUtils.isNotEmpty(valueInFile)) {
            Function<String, Object> resolver = customFieldMappers.get(col.getFieldCode());
            if (resolver != null) {
                return resolver.apply(valueInFile) != null;
            } else {
                return true;
            }
        }
        return false;
    }

    private List<ValidationError> validateFieldsPatterns(List<String> row,
                                                         Map<AuditedFieldCode, Function<String, Object>> customFieldMappers){
        List<ValidationError> errors = new ArrayList<>();
        PerformanceMonitor.before(PerformanceMonitor.Action.STEP_A);
        errors.addAll(validateFieldValues(row));
        PerformanceMonitor.after(PerformanceMonitor.Action.STEP_A);

        PerformanceMonitor.before(PerformanceMonitor.Action.STEP_B);
        errors.addAll(FieldPatternValidator.validateAttributesPatterns(
                fieldCode -> indexes.forRecord().getSingleColumnValue(fieldCode, row), customFieldMappers));
        PerformanceMonitor.after(PerformanceMonitor.Action.STEP_B);

        PerformanceMonitor.before(PerformanceMonitor.Action.STEP_C);
        for (ChildType childType : ChildType.values()) {
            int childIndex = 0;
            for (EntityFieldIndexes childFieldIndexes : indexes.forChild(childType)) {
                errors.addAll(validateChildPattern(childType, fieldCode -> childFieldIndexes.getSingleColumnValue(fieldCode, row), childIndex));
                childIndex ++;
            }
        }
        PerformanceMonitor.after(PerformanceMonitor.Action.STEP_C);

        log.trace("Fields patterns validation completed with {} errors", errors.size());
        return errors;
    }

    private List<ValidationError> validateChildPattern(ChildType childType, Function<AuditedFieldCode, String> childFields, Integer childIndex) {
        switch (childType) {
            case IDENTITY: return FieldPatternValidator.validateIdentityDocumentPatterns(childFields,childIndex,
                    customFieldMappers.get(AuditedFieldCode.RECORD_IDENTITY_DOCUMENT_TYPE));
            case PERSON_BIOGRAPHY: return FieldPatternValidator.validateDateDetailsPattern(childFields, childIndex);
            case FUNCTION: return FieldPatternValidator.validateFunctionPatterns(childFields, childIndex);
            case SANCTION_REFERENCE: return FieldPatternValidator.validateSanctionReferencePatterns(childFields, childIndex);
            default: return emptyList();
        }
    }

    private List<ValidationError> validateFieldValues(List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        log.trace("Validating referential in line {}", row);
        for (AuditedFieldCode fieldCode : indexes.forRecord().getFieldCodes()) {
            for (String columnValue : indexes.forRecord().getAllColumnValues(fieldCode, row)) {
                log.trace("Validating referential value, AFC={}: {}", fieldCode, columnValue);
                if (columnValue != null) {
                    Function<String, ?> fieldResolver = customFieldMappers.get(fieldCode);
                    if (fieldResolver != null) {
                        resolveFieldCollectingErrors(columnValue, fieldResolver, errors);
                    }
                }
            }
        }
        return errors;
    }

    private void resolveFieldCollectingErrors(String columnValue, Function<String, ?> fieldResolver, List<ValidationError> errors) {
        try {
            fieldResolver.apply(columnValue);
        } catch (ReglissException e) {
            errors.add(new ValidationError(e.getErrorCode(), e.getParameters()));
        }
    }


    private List<ValidationError> decorateErrorsWithColumnNamesInRow(List<ValidationError> errorCodes, MappingFile mapping) {
        return errorCodes.stream()
                .map(importError -> new ValidationError(importError.getErrorCode(),
                        determineColumnLabel(importError.getParameters()[0], mapping.getColumns())))
                .collect(toList());
    }

    private String determineColumnLabel(String fieldCodeString, List<MappingColumn> columns) {
        AuditedFieldCode fieldCode = AuditedFieldCode.valueOf(fieldCodeString);
        return columns.stream()
            .filter(column -> column.getFieldCode().equals(fieldCode))
            .findAny()
            .map(MappingColumn::getColumnLabel)
            .orElseThrow(() -> new ReglissException("Field not defined in mapping: " + fieldCodeString));
    }

}

