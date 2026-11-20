package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.annotation.AuditedFieldCode;
import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("!" + MappingFileValidator.STRONG_PROFILE)
public class MappingFileValidatorWeak implements MappingFileValidator {

    @Override
    public List<ValidationError> validate(MappingFile mappingFile) {
        List<AuditedFieldCode> allPossibleRequiredColumns = FieldIntegrityValidatorWeak.getAllPossibleRequiredColumns();
        return FieldIntegrityValidatorWeak.validateMinimumRequiredFieldsArePresentForMapping(
                mappingFile.getColumns(), allPossibleRequiredColumns);
    }

    @Override
    public RecordValidator createRecordValidator() {
        return new RecordValidatorWeak();
    }
}
