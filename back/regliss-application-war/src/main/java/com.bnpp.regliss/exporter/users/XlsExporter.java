package com.bnpp.regliss.exporter.users;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class XlsExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(XlsExporter.class);

    private final OutputStream outStream;
    Workbook workbook = new XSSFWorkbook();
    Sheet testSheet;
    public XlsExporter(OutputStream outputStream, String sheetName) {
        this.outStream = outputStream;
        this.testSheet = workbook.createSheet(sheetName);
    }

    public void writeWorkbook() {
        try {
            workbook.write(outStream);
        } catch (IOException e) {
            LOGGER.info("Write workbook exception");
            LOGGER.debug("Write workbook exception", e);
        }
    }

    public void writeLine(int rowNumber, Object... cells) { writeXLSRowLine(testSheet, rowNumber, cells); }

    public void writeXLSRowLine(Sheet sheet, int rowNumber, Object... cells) {
        createNewRow(sheet, toXlsLine(cells), rowNumber);
    }

    private static String toXlsLine(Object... cells) {
        return Stream.of(cells).map(XlsExporter::cellToString).collect(joining(";"));
    }

    public static void createNewRow(Sheet sheet, String rowContent, int rowNumber) {
        String[] arr = rowContent.split(";");
        Row row = sheet.createRow(rowNumber);
        for (int i = 0; i < arr.length; i++) {
            row.createCell(i).setCellValue((arr[i]));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String cellToString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Optional) {
            Optional opt = (Optional) value;
            return opt.orElse("").toString();
        }
        return value.toString();
    }
}

