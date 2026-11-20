package com.bnpp.regliss.importer.manual.validation;

import com.bnpp.regliss.entity.MappingFile;
import com.bnpp.regliss.entity.ValidationError;

import java.util.List;

public interface RecordValidator {

    List<ValidationError> validateRecord(MappingFile mapping, List<String> row);
}
