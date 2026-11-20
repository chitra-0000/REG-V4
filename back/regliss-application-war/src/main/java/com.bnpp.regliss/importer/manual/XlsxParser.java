package com.bnpp.regliss.importer.manual;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class XlsxParser {

    public static List<ParsedLine> readFile(InputStream inputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            return parseLines(workbook);
        }
    }

    private static List<ParsedLine> parseLines(Workbook workbook) {
        List<ParsedLine> rows = new ArrayList<>();
        if (workbook.getNumberOfSheets() == 0) {
            return emptyList();
        }
        int numberOfCellsInHeader = 0;
        for (Row row : workbook.getSheetAt(0)) {
            List<String> currentRow = new ArrayList<>();
            if (row.getRowNum() == 0) {
                numberOfCellsInHeader = row.getLastCellNum();
            }
            int numberOfCellsInCurrentRow = row.getLastCellNum();
            if (numberOfCellsInCurrentRow < numberOfCellsInHeader) {
                numberOfCellsInCurrentRow = numberOfCellsInHeader;
            }
            for (int i = 0; i < numberOfCellsInCurrentRow; i++) {
                currentRow.add(getCellValue(row.getCell(i)));
            }
            rows.add(new ParsedLine(row.getRowNum() + 1, currentRow));
        }
        return rows;
    }

    private static String getCellValue(Cell cell) {
        DataFormatter df = new DataFormatter();
        return StringUtils.isBlank(df.formatCellValue(cell)) ? null : df.formatCellValue(cell);
    }

    private XlsxParser(){}
}

