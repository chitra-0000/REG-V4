package com.bnpp.regliss.exporter.writer.format;

// [MISSING FROM SCREENSHOTS: lines 3-13 of the original file were not visible in the photos]

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public abstract class FileExportData {
    private final Version version;

    public abstract Set<Long> getRecordsIds();
    public abstract String getExportType();

    public abstract List<? extends ExportFormatWriter> getWritersForRecord(Long recordId, List<? extends ExportFormatWriter> pagedFormatWriters);

    public Confidentiality getListConfidentiality() { return version.getList().getConfidentiality(); }

    public boolean hasNoRecordsToExport() { return getRecordsIds().isEmpty(); }

    public int getTotalNumberOfRecords() { return getRecordsIds().size(); }

    public Map<CounterKeys,Long> computeStatistics(Long filterId){
        if (filterId != null) {
            // do nothing
        }
        return new EnumMap<>(CounterKeys.class);
    }

}

