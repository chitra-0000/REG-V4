package com.bnpp.regliss.importer.manual;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

import org.apache.commons.lang.StringUtils;

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
