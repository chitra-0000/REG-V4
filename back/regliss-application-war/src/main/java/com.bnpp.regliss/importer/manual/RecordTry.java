package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class RecordTry<T> {
    private T value;
    private LineImportErrors errors;

    public RecordTry(T record) { this.value = record; }

    public RecordTry(LineImportErrors errors) {
        if (errors.getErrors().isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.errors = errors;
    }

    public static <T> Collection<LineImportErrors> extractErrors(List<RecordTry<T>> tryList) {
        return tryList.stream().filter(RecordTry::hasErrors).map(RecordTry::getErrors).collect(toList());
    }

    public T getValue() { return value; }

    public LineImportErrors getErrors() { return errors; }

    public boolean isOk() { return errors == null; }

    public boolean hasErrors() { return !isOk(); }
}
