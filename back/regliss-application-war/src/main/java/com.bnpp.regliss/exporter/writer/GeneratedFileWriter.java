package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.entity.GeneratedFileFormat;
import com.bnpp.regliss.entity.SubstitutionMatrixElement;
import com.bnpp.regliss.entity.referential.ListReferential;
import com.bnpp.regliss.entity.referential.record.RecordReferential;
import com.bnpp.regliss.exporter.writer.FlatFileWriter.FlatDelimiters;
import com.bnpp.regliss.service.SubstitutionMatrixService;
import lombok.Data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;


public class GeneratedFileWriter implements AutoCloseable {
    @Data
    public static class Delimiters {
        public static final Delimiters NO_DELIMITERS = new Delimiters("", "", "\n", "");
        private final String interCellDelimiter; // semicolon
        private final String intraCellDelimiter; // ,
        private final String newLineDelimiter; // \n
        private final String cellQuote; // "
    }

    private final FlatFileWriter flatFileWriter;
    private final String intraCellDelimiter;
    private final GeneratedFileFormat fileFormat;
    private final String newLineDelimiter;

    private List<SubstitutionMatrixElement> substitutionMatrix;

    public GeneratedFileWriter(Writer writer, Delimiters delimiters, List<SubstitutionMatrixElement> substitutionMatrix,
                               GeneratedFileFormat fileFormat) {
        FlatDelimiters flatDelimiters = new FlatDelimiters(delimiters.interCellDelimiter, delimiters.newLineDelimiter, delimiters.cellQuote);
        this.flatFileWriter = new FlatFileWriter(writer, flatDelimiters);
        this.intraCellDelimiter = delimiters.intraCellDelimiter;
        this.newLineDelimiter = delimiters.newLineDelimiter;
        this.substitutionMatrix = new ArrayList<>(substitutionMatrix);
        this.fileFormat = fileFormat;
    }

    public void addCell(Stream<String> manyValues) { addCell(manyValues.collect(joining(intraCellDelimiter))); }
    public void addCell(Stream<String> manyValues,int maxlength) {
        addCell(manyValues.collect(joining(intraCellDelimiter)),maxlength);
    }

    public void addCell(Object something) { flatFileWriter.addStringCell(getStringValue(something)); }
    public void addUnqotedStringCell(Object something) { flatFileWriter.addUnqotedStringCell(getStringValue(something)); }

    public void addCell(Object something, int maxLength) {
        String cellValue=getStringValue(something);
        if(cellValue.length()>maxLength) {
            cellValue=cellValue.substring(0,maxLength);
        }
        flatFileWriter.addStringCell(cellValue);
    }
    public void writeLine() { flatFileWriter.writeLine(); }

    public GeneratedFileFormat getFileFormat() { return fileFormat; }

    private String getStringValue(Object fieldValue) {
        if (fieldValue == null) {
            return "";
        }
        if (fieldValue instanceof String || fieldValue instanceof Enum<?>) {
            return SubstitutionMatrixService.applySubstitutionMatrix(fieldValue.toString(), substitutionMatrix);
        }
        if (fieldValue instanceof ListReferential) {
            return SubstitutionMatrixService.applySubstitutionMatrix(((ListReferential)fieldValue).getLabel(), substitutionMatrix);
        }
        if (fieldValue instanceof RecordReferential) {
            return SubstitutionMatrixService.applySubstitutionMatrix(((RecordReferential)fieldValue).getLabel(), substitutionMatrix);
        }
        if (fieldValue instanceof LocalDateTime || fieldValue instanceof LocalDate) {
            throw new IllegalArgumentException("Dates must be formated inline");
        }
        if (fieldValue instanceof Optional) {
            Optional<?> opt = (Optional<?>) fieldValue;
            return getStringValue(opt.orElse(null));
        }

            return SubstitutionMatrixService.applySubstitutionMatrix(String.valueOf(fieldValue), substitutionMatrix);
    }

    public void write(String value) {
        try {
            flatFileWriter.getWriter().write(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeLine(String value) {
        try {
            flatFileWriter.getWriter().write(value);
            flatFileWriter.getWriter().write(this.newLineDelimiter);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    @Override
    public void close() { flatFileWriter.close(); }

    public static String toDay(LocalDateTime date) { return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date); }


}
