package com.bnpp.regliss.exporter.writer.format.addmodsup;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.NoSuchElementException;
import java.util.Optional;

public class POIExtension {

    private static final String COULD_NOT_FIND_TAG = "Could not find tag \"";
    private static final String IN_THE_SHEET = "\" in the sheet ";

    public static void clearColumn(Sheet sheet, int columnIndex) {
        for (Row row : sheet) {
            Cell cell = row.getCell(columnIndex);
            if (cell != null)
                cell.setCellValue("");
        }
    }

    public static void replaceFirstOccurenceOfTagInRow(Sheet sheet, int startRow, String tagName, String newValue) {
        Cell cell = findFirstOccurenceOfCellInRow(sheet, startRow, tagName);
        if (cell != null) {
            cell.setCellValue(newValue);
        }
    }

    public static void replaceFirstOccurenceOfTagInRow(Sheet sheet, int startRow, String tagName, int newValue) {
        Cell cell = findFirstOccurenceOfCellInRow(sheet, startRow, tagName);
        if (cell != null) {
            cell.setCellValue(newValue);
        }
    }

    /*
     * This method replaces a specific tag with an integer value, by using the row
     * by row search. This is very slow when the sheet grows big, so if the row is
     * known, please use the specific method replaceFirstOccurenceOfTagInRo
     */
    public static void replaceFirstOccurenceOfTag(Sheet sheet, int startRow, String tagName, int newValue) {
        Cell cell = findFirstOccurenceOfCellFromRow(sheet, startRow, tagName);
        cell.setCellValue(newValue);
    }

    /*
     * This method replaces a specific tag with a String value, by using the row by
     * row search. This is very slow when the sheet grows big, so if the row is
     * known, please use the specific method replaceFirstOccurenceOfTagInRo
     */
    public static void replaceFirstOccurenceOfTag(XSSFSheet sheet, int startRow, String tagName, String newValue) {
        Cell cell = findFirstOccurenceOfCellFromRow(sheet, startRow, tagName);
        cell.setCellValue(newValue);
    }

    /*
     * This method searches for a specific tag, starting from the startRow, until
     * the end of the sheet. If the values is found, a reference to the cell is
     * returned; Otherwise, a NoSuchElementException exception is thrown Useful when
     * you don't know where the tag is located (line or column), but very slow when
     * the sheet grows big
     */

    private static Cell findFirstOccurenceOfCellFromRow(Sheet sheet, int startRow, String cellContent) {
        for (Row row : sheet) {
            if (row.getRowNum() < startRow)
                continue;
            Optional<Cell> cell = findFirstCellInRowWithContent(cellContent, row);
            if(cell.isPresent()) {
                return cell.get();
            }
        }
        throw new NoSuchElementException(
                COULD_NOT_FIND_TAG + cellContent + IN_THE_SHEET + sheet.getSheetName() + "\".");
    }

    public static Cell findFirstOccurenceOfCellInColumn(Sheet sheet, int startRow, int columnNo, String cellContent) {
        for (Row row : sheet) {
            if (row.getRowNum() < startRow)
                continue;
            Cell cell = row.getCell(columnNo);
            if(cellHasContent(cellContent, cell)) {
                return cell;
            }
        }
        throw new NoSuchElementException(
                COULD_NOT_FIND_TAG + cellContent + IN_THE_SHEET + sheet.getSheetName() + "\".");
    }

    private static Cell findFirstOccurenceOfCellInRow(Sheet sheet, int startRow, String cellContent) {
        Row row = sheet.getRow(startRow);
        return findFirstCellInRowWithContent(cellContent, row)
                .orElseThrow(() -> new NoSuchElementException(COULD_NOT_FIND_TAG + cellContent + IN_THE_SHEET + sheet.getSheetName() + "\"."));
    }

    private static Optional<Cell> findFirstCellInRowWithContent(String cellContent, Row row) {
        for (Cell cell : row) {
            if(cellHasContent(cellContent, cell)) {
                return Optional.of((Cell)cell);
            }
        }
        return Optional.empty();
    }

    private static boolean cellHasContent(String cellContent, Cell cell) {
        return cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING
                && cell.getRichStringCellValue().getString().trim().equals(cellContent);
    }

    public static void copyRowsInParallel(Sheet worksheet, Row sourceRowOdd, Row sourceRowEven,
            int destinationRowNumStart, int numberOfRows) {

        for (int rowNum = destinationRowNumStart; rowNum < destinationRowNumStart + numberOfRows; rowNum++) {
            Row sourceRow = getSourceRow(rowNum, sourceRowOdd, sourceRowEven);
            Row newRow = getNewRow(worksheet, rowNum);

            // Loop through source columns to add to new row
            for (int i = 1; i < sourceRow.getLastCellNum(); i++) { // Start from 1, do not copy the first column
                Cell sourceCell = sourceRow.getCell(i);
                if (sourceCell != null) {
                    Cell newCell = newRow.createCell(i);
                    // The clone() create unnecessary objects and slows a lot the processing
                    newCell.setCellStyle(sourceCell.getCellStyle());
                    newCell.setCellType(sourceCell.getCellType());
                    setCellValueBasedOnType(sourceCell, newCell);
                }
            }


        }
    }

    private static void setCellValueBasedOnType(Cell oldCell, Cell newCell) {
        // Set the cell data value
        switch (oldCell.getCellType()) {
        case Cell.CELL_TYPE_BLANK:
            newCell.setCellValue(oldCell.getStringCellValue());
            break;
        case Cell.CELL_TYPE_BOOLEAN:
            newCell.setCellValue(oldCell.getBooleanCellValue());
            break;
        case Cell.CELL_TYPE_ERROR:
            newCell.setCellErrorValue(oldCell.getErrorCellValue());
            break;
        case Cell.CELL_TYPE_FORMULA:
            newCell.setCellFormula(oldCell.getCellFormula());
            break;
        case Cell.CELL_TYPE_NUMERIC:
            newCell.setCellValue(oldCell.getNumericCellValue());
            break;
        case Cell.CELL_TYPE_STRING:
            newCell.setCellValue(oldCell.getRichStringCellValue());
            break;
        default:
            throw new IllegalArgumentException("Cell type not supported: " + oldCell.getCellType());
        }
    }

    private static Row getNewRow(Sheet worksheet, int rowNum) {
        Row newRow = worksheet.getRow(rowNum);
        // If the row exist in destination, push down all rows by 1 else create a new row
        if (newRow != null) {
            worksheet.shiftRows(rowNum, worksheet.getLastRowNum(), 1);
        } else {
            newRow = worksheet.createRow(rowNum);
        }
        return newRow;
    }

    private static Row getSourceRow(int rowNum, Row sourceRowOdd, Row sourceRowEven) {
        if (rowNum % 2 == 0) {
            return sourceRowEven;
        } else {
            return  sourceRowOdd;
        }
    }

    private POIExtension(){}
}

