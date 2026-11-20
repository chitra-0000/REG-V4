package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import com.bnpp.regliss.MappingColumn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
@Profile(MappingFileValidator.STRONG_PROFILE)
public class MappingFileValidatorStrong implements MappingFileValidator {

    @Override
    public List<ValidationError> validate(MappingFile mappingFile) {
        List<AuditedFieldCode> allPossibleRequiredColumns = FieldIntegrityValidatorStrong.getAllPossibleRequiredColumns();
        List<ValidationError> errors = FieldIntegrityValidatorStrong.validateMinimumRequiredFieldsArePresent(
                mappingFile.getColumns(), allPossibleRequiredColumns);
        errors.addAll(validateChildrenIntegrity(mappingFile));
        return errors;
    }

    @Override
    public RecordValidator createRecordValidator() {
        return new RecordValidatorStrong();
    }

    private List<ValidationError> validateChildrenIntegrity(MappingFile mappingFile) {
        return mappingFile.getAllChildrenColumns().stream()
                .flatMap(childCategory -> validateChildIntegrity(childCategory, mappingFile))
                .collect(toList());
    }

    private Stream<ValidationError> validateChildIntegrity(AuditedFieldCode.ChildCategory childCategory,
                                                           MappingFile mappingFile) {
        switch (childCategory) {
            case ADDRESS:
                return FieldIntegrityValidatorStrong.validateAddress(mappingFile.getColumns()).stream();
            case ALIAS:
                return FieldIntegrityValidatorStrong.validateAlias(mappingFile.getColumns()).stream();
            case PROGRAM:
                return FieldIntegrityValidatorStrong.validateProgramLabel(mappingFile.getColumns()).stream();
            case IDENTITY:
                return FieldIntegrityValidatorStrong.validateIdentityDocument(mappingFile.getColumns()).stream();
            case PERSON_BIOGRAPHY:
                return FieldIntegrityValidatorStrong.validateBiography(mappingFile.getColumns()).stream();
            case COUNTRY_DETAILS:
                return FieldIntegrityValidatorStrong.validateNationality(mappingFile.getColumns()).stream();
            default:
                return Stream.empty();
        }
    }
}
