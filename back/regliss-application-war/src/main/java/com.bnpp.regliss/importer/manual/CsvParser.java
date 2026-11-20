package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-15 of the original file were not visible in the photos]

public class CsvParser {

    private CsvParser(){}

    public static List<ParsedLine> readCsvFull(InputStream inputStream) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(createIndexer())
                    .filter(line -> StringUtils.isNotBlank(line.value))
                    .map(CsvParser::parseLine)
                    .collect(toList());
        }
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
        List<String> cells = Stream.of(rawLine.getValue().split(delimiter, -1))
                .map(s -> StringUtils.isBlank(s) ? null : s)
                .collect(toList());
        return new ParsedLine(rawLine.getIndex() + 1, cells);
    }

}

