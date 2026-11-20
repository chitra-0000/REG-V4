package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-13 of the original file were not visible in the photos]

@Service
@Profile("!" + MappingFileValidator.STRONG_PROFILE)
public class MappingFileValidatorWeak implements MappingFileValidator {

    public List<ValidationError> validate(MappingFile mapping){
        List<MappingColumn> columnsWithRequiredFields = FieldIntegrityValidatorWeak.getAllPossibleRequiredColumns(mapping);

        return FieldIntegrityValidatorWeak.validateMinimumRequiredFieldsArePresentForMapping(columnsWithRequiredFields);
    }

    @Override
    public RecordValidator createRecordValidator(Map<AuditedFieldCode, Function<String, Object>> customFieldMappers,
                                                 RecordFieldIndexes indexes) {
        return new RecordValidatorWeak(customFieldMappers, indexes);
    }

}

