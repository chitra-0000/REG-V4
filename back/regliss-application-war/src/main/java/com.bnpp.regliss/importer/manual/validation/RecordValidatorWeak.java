package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

@Slf4j
public class RecordValidatorWeak implements RecordValidator {

    private final Map<AuditedFieldCode, Function<String, Object>> customFieldMappers;
    private final RecordFieldIndexes indexes;

    public RecordValidatorWeak(Map<AuditedFieldCode, Function<String, Object>> customFieldMappers, RecordFieldIndexes indexes) {
        this.customFieldMappers = customFieldMappers;
        this.indexes = indexes;
    }

    public List<ValidationError> validateRecord(MappingFile mapping, List<String> row) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(validateFieldsIntegrity(mapping, row));
        errors.addAll(validateFieldsPatterns(row));
        return errors;
    }

    private List<ValidationError> validateFieldsIntegrity(MappingFile mapping, List<String> row){
        List<ValidationError> errors = FieldIntegrityValidatorWeak.validateMinimumRequiredFieldsArePresentForImport(mapping, row);
        log.trace("Fields integrity validation completed with {} errors", errors.size());
        return errors;
    }

    private List<ValidationError> validateFieldsPatterns(List<String> row){
        List<ValidationError> errors = new ArrayList<>();
        PerformanceMonitor.before(PerformanceMonitor.Action.STEP_A);
        errors.addAll(validateFieldValues(row));
        PerformanceMonitor.after(PerformanceMonitor.Action.STEP_A);

        log.trace("Fields patterns validation completed with {} errors", errors.size());
        return errors;
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

}

