package com.bnpp.regliss.importer.manual.validation;

// [MISSING FROM SCREENSHOTS: lines 3-19 of the original file were not visible in the photos]

@Service
@Profile(MappingFileValidator.STRONG_PROFILE)
public class MappingFileValidatorStrong implements MappingFileValidator {
    @Override
    public List<ValidationError> validate(MappingFile mapping){
        List<MappingColumn> columnsWithRequiredFields = FieldIntegrityValidatorStrong.getAllPossibleRequiredColumns(mapping);
        Set<AuditedFieldCode> fieldCodes = columnsWithRequiredFields.stream()
                .map(MappingColumn::getFieldCode).collect(toSet());

        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(FieldIntegrityValidatorStrong.validateMinimumRequiredFieldsArePresent(fieldCodes));
        errors.addAll(validateChildrenIntegrity(mapping));
        return errors;
    }

    @Override
    public RecordValidator createRecordValidator(Map<AuditedFieldCode, Function<String, Object>> customFieldMappers,
                                                 RecordFieldIndexes indexes) {
        return new RecordValidatorStrong(customFieldMappers, indexes);
    }

    private List<ValidationError> validateChildrenIntegrity(MappingFile mapping) {
        return mapping.getAllChildrenColumns().stream()
                .flatMap(c -> validateChildIntegrity(c).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationError> validateChildIntegrity(List<MappingColumn> aChildColumns) {
        AuditedFieldCode.SemiAutoImportFieldCategory childCategory = aChildColumns.get(0).getFieldCode().getCategory();
        Integer childIndex = aChildColumns.get(0).getChildIndex();
        Set<AuditedFieldCode> actualChildColumns = aChildColumns.stream().map(MappingColumn::getFieldCode).collect(toSet());

        switch (childCategory) {
            case ADDRESS: return FieldIntegrityValidatorStrong.validateAddress(actualChildColumns, childIndex);
            case ALIAS: return FieldIntegrityValidatorStrong.validateAlias(actualChildColumns, childIndex);
            case PROGRAM: return FieldIntegrityValidatorStrong.validateProgramLabel(actualChildColumns, childIndex);
            case IDENTITY: return FieldIntegrityValidatorStrong.validateIdentityDocument(actualChildColumns, childIndex);
            case PERSON_BIOGRAPHY: return FieldIntegrityValidatorStrong.validateBiography(actualChildColumns, childIndex);
            case COUNTRY_DETAILS: return FieldIntegrityValidatorStrong.validateNationality(actualChildColumns, childIndex);
            default: return emptyList();
        }
    }
}

