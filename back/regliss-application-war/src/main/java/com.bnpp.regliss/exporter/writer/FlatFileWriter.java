package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.ReglissException;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class FlatFileWriter implements AutoCloseable {
    @Data
    public static class FlatDelimiters {
        private final String interCellDelimiter; // semicolon
        private final String newLineDelimiter; // \n
        private final String cellQuote; // "
    }

    private final Writer writer;
    private final FlatDelimiters delimiters;
    private List<String> lineInProgress = new ArrayList<>();
    private boolean hasContent;

    public FlatFileWriter(Writer writer, FlatDelimiters delimiters) {
        this.writer = writer;
        this.delimiters = delimiters;
    }
    public void addStringCell(String something) {
        hasContent = true;
        String somethingAsString = delimiters.cellQuote + normalizeCell(something) + delimiters.cellQuote;
        lineInProgress.add(somethingAsString);
    }
    public void addUnqotedStringCell(String something) {
        hasContent = true;
        String somethingAsString = normalizeCell(something);
        lineInProgress.add(somethingAsString);
    }
    private String normalizeCell(String cellContent) {
        if (StringUtils.isEmpty(delimiters.cellQuote)) {
            return cellContent.replace(delimiters.newLineDelimiter, " ").trim();
        } else {
            return cellContent.replace(delimiters.cellQuote, " ").trim();
        }
    }

    public void writeLine() {
        try {
            writer.write(lineInProgress.stream().collect(joining(delimiters.interCellDelimiter)) + delimiters.newLineDelimiter);
            lineInProgress.clear();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Writer getWriter() { return writer; }

    @Override
    public void close() {
        if (!hasContent) {
            writeLine();
        }
        try {
            writer.close();
        } catch (IOException e) {
            throw new ReglissException(e, "Could not close");
        }
    }
}

