package com.bnpp.regliss.importer.manual;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class CsvParser {

    private CsvParser() {
    }

    public static List<ParsedLine> readCsvFull(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        return reader.lines()
                .map(createIndexer())
                .filter(line -> StringUtils.isNotBlank(line.value))
                .map(CsvParser::parseLine)
                .collect(toList());
    }

    private static class IndexedItem<T> {
        private final int index;
        private final T value;
        public IndexedItem(int index, T value) {
            this.index = index;
            this.value = value;
        }
        public int getIndex() { return index; }
        public T getValue() { return value; }
    }

    private static <T> Function<T, IndexedItem<T>> createIndexer() {
        Holder<Integer> ih = new Holder<>(0);
        return t -> new IndexedItem<>(ih.value++, t);
    }

    private static ParsedLine parseLine(IndexedItem<String> rawLine) {
        // -1 keeps trailing empty columns
        String delimiter = ";";
        String[] columns = rawLine.value.split(delimiter, -1);
        List<String> cells = Stream.of(columns)
                .map(s -> StringUtils.isBlank(s) ? null : s)
                .collect(toList());
        return new ParsedLine(rawLine.index + 1, cells);
    }
}
