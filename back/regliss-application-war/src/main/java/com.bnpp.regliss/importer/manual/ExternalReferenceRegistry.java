package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-12 of the original file were not visible in the photos]

public class ExternalReferenceRegistry {
    private final Map<String, Integer> seen = new HashMap<>();
    private final List<LineImportErrors> errors = new ArrayList<>();
    private final int columnIndex;

    public ExternalReferenceRegistry(MappingFile mapping) {
        this.columnIndex = extractExternalReferenceColumnIndex(mapping);
    }

    public ExternalReferenceRegistry(Integer externalReferenceColumnIndex) {
        this.columnIndex = externalReferenceColumnIndex;
    }

    private int extractExternalReferenceColumnIndex(MappingFile mapping) {
        MappingColumn externalReferenceMapping = mapping.getColumns().stream()
                .filter(column -> column.getFieldCode() == AuditedFieldCode.RECORD_EXTERNAL_REFERENCE)
                .findFirst()
                .orElseThrow(() -> new ReglissException(ReglissException.ErrorCode.NOT_FOUND));
        return externalReferenceMapping.getColumnIndex();
    }

    public Optional<LineImportErrors> addAndTestIfNew(ParsedLine line) {
        String externalReference = line.cells.get(columnIndex);
        if (externalReference == null) {
            return empty();
        }
        if (!seen.containsKey(externalReference)) {
            seen.put(externalReference, line.lineNumber);
            return empty();
        } else {
            ValidationError validationError = new ValidationError(ReglissException.ErrorCode.IMPORT_DUPLICATE_EXTERNAL_REFERENCE,
                    externalReference);
            List<Integer> linesPair = asList(seen.get(externalReference), line.lineNumber);
            LineImportErrors error = new LineImportErrors(linesPair, validationError);
            errors.add(error);
            return Optional.of(error);
        }
    }

    public List<LineImportErrors> getErrors() { return new ArrayList<>(errors); }

    public Set<String> getSeenExternalReferences() { return seen.keySet(); }
}

