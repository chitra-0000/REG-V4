package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]

public class CsvReader extends FlatDataSource {


    public CsvReader(InputStream inputStream) { super(inputStream); }

    private static final String DELIMITER = ";";

    @Override
    protected List<String> parseCells(String line) {
        return Stream.of(line.split(DELIMITER, -1))
                .map(s -> StringUtils.isBlank(s) ? null : s)
                .collect(toList());
    }

}

