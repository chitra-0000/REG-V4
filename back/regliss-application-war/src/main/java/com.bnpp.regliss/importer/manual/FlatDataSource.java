package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-12 of the original file were not visible in the photos]

public abstract class FlatDataSource implements Iterator<ParsedLine>, AutoCloseable {

    private final BufferedReader reader;

    public FlatDataSource(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.lineIndex = 0;
    }

    private String nextLine;
    private int lineIndex;

    @Override
    public boolean hasNext() {
        try {
            if (nextLine == null) {
                nextLine = reader.readLine();
            }
        } catch (IOException e) {
            throw new ReglissException(e);
        }
        return nextLine != null;
    }

    @Override
    public ParsedLine next() {
        if (hasNext()) {
            List<String> cells = parseCells(nextLine);
            nextLine = null;
            return new ParsedLine(lineIndex++, cells);
        } else {
            throw new NoSuchElementException();
        }
    }

    protected abstract List<String> parseCells(String nextLine);

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            throw new ReglissException(e);
        }
    }

    public int nrOfLines() { return (int) reader.lines().count(); }

}

