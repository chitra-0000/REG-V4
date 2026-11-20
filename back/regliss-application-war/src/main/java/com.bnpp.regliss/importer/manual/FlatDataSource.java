package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.ReglissException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

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

    public int nrOfLines() { return lineIndex; }

    @Override
    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
