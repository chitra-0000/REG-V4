package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;

import java.util.List;

public interface MappingFileValidator {

    String STRONG_PROFILE = "strong-validation";

    List<ValidationError> validate(MappingFile mappingFile);

    RecordValidator createRecordValidator();
}
