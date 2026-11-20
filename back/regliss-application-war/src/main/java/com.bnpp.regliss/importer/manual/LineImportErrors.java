package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.entity.ValidationError;

import java.util.Arrays;
import java.util.List;

public class LineImportErrors {

    private final String lineNumber;
    private final List<ValidationError> errors;

    public LineImportErrors(int lineNumber, List<ValidationError> errors) {
        this.lineNumber = String.valueOf(lineNumber);
        this.errors = errors;
    }

    public LineImportErrors(int lineNumber, ValidationError... errors) {
        this.lineNumber = String.valueOf(lineNumber);
        this.errors = Arrays.asList(errors);
    }

    public LineImportErrors(List<Integer> lineNumbers, ValidationError... errors) {
        this.lineNumber = lineNumbers.toString();
        this.errors = Arrays.asList(errors);
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
