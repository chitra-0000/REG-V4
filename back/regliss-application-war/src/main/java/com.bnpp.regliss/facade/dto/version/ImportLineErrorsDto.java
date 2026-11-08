package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-4 of the original file were not visible in the photos]

public class ImportLineErrorsDto {

    public String lineNumber;
    public List<String> errors = new ArrayList<>();
    public ImportLineErrorsDto() {}
    public ImportLineErrorsDto(int lineNumber, List<String> errors) { this(String.valueOf(lineNumber), errors); }

    public ImportLineErrorsDto(String lineNumber, List<String> errors) {
        this.lineNumber = lineNumber;
        this.errors = errors;
    }

    @Override
    public String toString() { return "#"+lineNumber + " - " + errors; }

}

