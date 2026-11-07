package com.bnpp.regliss.exporter.writer.format.addmodsup;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.batch.service.ExportPhase;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.bnpp.regliss.exporter.writer.format.addmodsup.AddModSupExcelCopyHelper.*;
import static com.bnpp.regliss.exporter.writer.format.addmodsup.POIExtension.clearColumn;
import static com.bnpp.regliss.exporter.writer.format.addmodsup.POIExtension.findFirstOccurenceOfCellInColumn;
import static java.util.Arrays.asList;

@Slf4j
@Service
@ReglissBatchProfile
public class AddModSupExcelExporter {

    private static final int PAGE_SIZE = 500;
    private static final String SUMMARY_TAG="_SUMMARY";
    @Value("${export.add.mod.sup.excel.sheet.size}")
    private int exportExcelSheetSize;
    @Autowired
    private BatchProgressService progressService;

    public void generateExcelFile(File template, File tempFile, AddModSupExportData addModSupExcelParams, ExportJobParameters exportJobParameters) {
        long startTime = System.currentTimeMillis();
        long beginTime = startTime;
        copyTemplateToOutputFile(template, tempFile);

        try (FileInputStream resultInputStream = new FileInputStream(tempFile);
             XSSFWorkbook resultWorkbook = new XSSFWorkbook(resultInputStream);
             BufferedOutputStream resultOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
             SXSSFWorkbook wb = new SXSSFWorkbook(resultWorkbook, 100)) {


            migrateXSSSheetDataToAuxiliarNewSXSSSheet(wb, SheetsTypes.getList());

            startTime = System.currentTimeMillis();
            if (addModSupExcelParams.getTotalNumberOfElements() == 0) {
                progressService.incrementBatchExportProgressForWritingAllPagesAtOnceOrNoPages(ExportPhase.EXPORT_WRITE_RECORDS,
                        1, exportJobParameters);
            }

            fillSummary(wb, addModSupExcelParams, exportJobParameters);
            log.debug("Fill summary after {} second(s). ", (System.currentTimeMillis() - startTime) / 1000.01d);

            startTime = System.currentTimeMillis();
            fillDetails(wb, addModSupExcelParams, exportJobParameters);
            log.debug("Fill details after {} second(s). ", (System.currentTimeMillis() - startTime) / 1000.01d);

            if (addModSupExcelParams.getTotalNumberOfElements() % PAGE_SIZE != 0) {
                progressService.incrementBatchExportProgressForWritingPage(ExportPageType.RECORDS_FULLY_LOADED_PAGE, 1,
                        getTotalNumberOfExportedLines(addModSupExcelParams), PAGE_SIZE, exportJobParameters);
            }

            cleanupTemplate(wb);

            resultOutputStream.flush();
            wb.write(resultOutputStream);

            log.info("Test finished after {} second(s). ", (System.currentTimeMillis() - beginTime) / 1000);
        } catch (IOException e) {
            log.error("Cannot generate ADD/MOD/SUPP file at location {} : {}", tempFile.getAbsoluteFile(), e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Cannot generate ADD/MOD/SUPP file: {}", e.getMessage());
        }
    }

    private int getTotalNumberOfExportedLines(AddModSupExportData addModSupExcelParams) {
        return addModSupExcelParams.getTotalNumberOfElements() * 2;
    }

    private void fillSummary(SXSSFWorkbook resultWorkbook, AddModSupExportData addModSupExcelParams,
                             ExportJobParameters exportJobParameters) {
        Sheet templateSheet = resultWorkbook.getXSSFWorkbook().getSheet(SheetsTypes.SUMMARY.getName());
        Sheet summarySheet = resultWorkbook.getSheet(SheetsTypes.SUMMARY.getAux());

        // ****** TABLE 1 ******
        for (AddModSupCountLine elementCounts : addModSupExcelParams.getElementsCounts()) {
            fillSummaryCountTable(summarySheet, elementCounts, getElementCountTagName(elementCounts));
        }

        // ****** TABLE 2 ******
        for (AddModSupCountLine recordCounts : addModSupExcelParams.getRecordsCounts()) {
            fillSummaryCountTable(summarySheet, recordCounts, recordCounts.getElementTypeName());
        }

        // Clear all unecesary tables
        int tmpRowIndex = findFirstOccurenceOfCellInColumn(summarySheet, 0, 0, AddModSupAction.ADD + SUMMARY_TAG)
                .getRowIndex();
        clearNextRows(summarySheet, tmpRowIndex - 1);

        // ****** TABLE ADD ******
        summarySheet = fillSummaryDetailedTable(summarySheet, templateSheet, addModSupExcelParams, AddModSupAction.ADD, exportJobParameters);

        // ****** TABLE MOD ******
        summarySheet = fillSummaryDetailedTable(summarySheet, templateSheet, addModSupExcelParams, AddModSupAction.MOD,
            exportJobParameters);

        // ****** TABLE SUPP ******
        fillSummaryDetailedTable(summarySheet, templateSheet, addModSupExcelParams, AddModSupAction.SUP, exportJobParameters);
    }

    private Sheet fillSummaryDetailedTable(Sheet sheet, Sheet template, AddModSupExportData addModSupExcelParams, AddModSupAction action,
                                           ExportJobParameters exportJobParameters) {
        String sectionName = getTemplateActionName(action);
        // Find and copy headers in template rowIndex
        int tmpRowIndex = findFirstOccurenceOfCellInColumn(template, 0, 0, sectionName + SUMMARY_TAG).getRowIndex();
        copyHeadersDataFromTemplate(sheet, template, tmpRowIndex);
        // Complete record nr in title
        int headerRowIndex = findFirstOccurenceOfCellInColumn(sheet, 0, 0, sectionName + SUMMARY_TAG).getRowIndex();
        POIExtension.replaceFirstOccurenceOfTagInRow(sheet, headerRowIndex, "[" + sectionName + "_NB_RECORDS]",
                addModSupExcelParams.getDetailsRecordsCountsForAction(action).intValue());
        Integer numberOfRows = addModSupExcelParams.getDetailsElementsCountForAction(action);
        // Cleaning the tags must be done here (tags are not accessible later)
        clearColumn(sheet, 0);
        if (numberOfRows != 0) {
            SheetRowData rowData = buildRowDataForSummarySheet(template, sectionName);
            List<Map<String, String>> dataList = new ArrayList<>();
            addModSupExcelParams.getSummaryLinesForAction(action)
                    .forEach(summaryLine -> dataList.add(extractSummaryDataMap(action, summaryLine)));
            int startFinalRowNumber = headerRowIndex + 2;
            for (Map<String, String> summaryLine : dataList) {
                sheet = populateSummaryNextRowAndNextSheet(sheet, rowData, startFinalRowNumber++, summaryLine);
                int numberOfWrittenLines = startFinalRowNumber + getNumberOfSummaryLinesWrittenInPreviousDetailsTables
                        (addModSupExcelParams, action);
                incrementBatchExecutionForFullPageWritten(addModSupExcelParams, exportJobParameters, numberOfWrittenLines);
            }
        }
        return sheet;
    }

    private void fillDetails(SXSSFWorkbook resultWorkbook, AddModSupExportData addModSupExcelParams, ExportJobParameters exportJobParameters) {
        Sheet templateSheet = resultWorkbook.getXSSFWorkbook().getSheet(SheetsTypes.DETAILS.getName());
        Sheet detailsSheet = resultWorkbook.getSheet(SheetsTypes.DETAILS.getAux());
        // Need to clear before the stream
        clearColumn(detailsSheet, 0);
        int totalNumberOfAddModSups = addModSupExcelParams.getTotalNumberOfElements();
        if (totalNumberOfAddModSups != 0) {
            SheetRowData rowData = buildRowDataForDetailsSheet(templateSheet);
            int startRowNumber = rowData.getRowHeader().getRowNum();
            AtomicInteger atomicCurrentRowNumber = new AtomicInteger(startRowNumber);
            addModSupExcelParams.getDetailsLines()
                    .forEach(detailLine -> {
                        populateDetailsNextRowAndNextSheet(detailsSheet, rowData, atomicCurrentRowNumber.getAndIncrement(), detailLine);
                        int numberOfWrittenLines = atomicCurrentRowNumber.get() - startRowNumber;
                        incrementBatchExecutionForFullPageWritten(addModSupExcelParams, exportJobParameters, numberOfWrittenLines);

                    });
            }
        }

    private Sheet populateDetailsNextRowAndNextSheet(Sheet localSheet, SheetRowData rowData, int currentRowNumber, AddModSupLine detailLine) {
        //If over limit -> create a new localSheet
        if (currentRowNumber >= exportExcelSheetSize) {
            int sheetNo = currentRowNumber / exportExcelSheetSize;
            SXSSFWorkbook workbook = (SXSSFWorkbook) localSheet.getWorkbook();
            localSheet = getNextDetailsSheet(workbook, rowData.getTemplate(), sheetNo);
            currentRowNumber = currentRowNumber - exportExcelSheetSize * sheetNo + 1;
        }
        //Add row and values
        Row row = getRowInGivenSheet(localSheet, currentRowNumber);
        populateDetailsRowWithValues(row, detailLine, rowData.getKeyMaps(), rowData.getStyle());
        return localSheet;
    }

    private Sheet populateSummaryNextRowAndNextSheet(Sheet localSheet, SheetRowData rowData, int currentRowNumber,
                                                           Map<String, String> summaryLine) {
        //If over limit -> create a new localSheet
        if (currentRowNumber >= exportExcelSheetSize) {
            if (currentRowNumber % exportExcelSheetSize == 0) {
                SXSSFWorkbook workbook = (SXSSFWorkbook) localSheet.getWorkbook();
                int sheetNo = workbook.getSheetIndex(localSheet) - 1;
                localSheet = getNextSummarySheet(workbook, rowData.getTemplate(), rowData.getRowHeader(), sheetNo);
            }
            int sheetNoInvalid = currentRowNumber / exportExcelSheetSize;
            currentRowNumber = currentRowNumber - exportExcelSheetSize * sheetNoInvalid + 1;
        }
        //Add row and values
        Row tempRow = getRowInGivenSheet(localSheet, currentRowNumber);
        populateSummaryRowWithValues(summaryLine, tempRow, rowData.getKeyMaps(), rowData.getStyle(currentRowNumber));
        return localSheet;
    }

    private SXSSFSheet getNextSummarySheet(SXSSFWorkbook resultWorkbook, Sheet template, Row sourceHeader, int sheetNo) {
        SXSSFSheet sheet = (SXSSFSheet) resultWorkbook.getSheet(SheetsTypes.SUMMARY.getPage(sheetNo));
        if (sheet == null) {
            sheet = (SXSSFSheet) resultWorkbook.createSheet(SheetsTypes.SUMMARY.getPage(sheetNo));
            int position = resultWorkbook.getSheetIndex(SheetsTypes.DETAILS.getAux());
            resultWorkbook.setSheetOrder(SheetsTypes.SUMMARY.getPage(sheetNo), position);

            Row header = getRowInGivenSheet(sheet, 0);
            header.setHeight(sourceHeader.getHeight());
            copySheetDefaultStyle((XSSFSheet) template, sheet);
            copySheetRow(template, sheet, sourceHeader, header);
            clearColumn(sheet, 0);
        }
        return sheet;
    }

    private SXSSFSheet getNextDetailsSheet(SXSSFWorkbook resultWorkbook, Sheet template, int sheetNo) {
        SXSSFSheet sheet = (SXSSFSheet) resultWorkbook.getSheet(SheetsTypes.DETAILS.getPage(sheetNo));
        if (sheet == null) {
            sheet = (SXSSFSheet) resultWorkbook.createSheet(SheetsTypes.DETAILS.getPage(sheetNo));
            Row sourceHeader = findFirstOccurenceOfCellInColumn(template, 0, 0, "ALL_HEADER").getRow();
            Row header = getRowInGivenSheet(sheet, 0);
            copySheetDefaultStyle((XSSFSheet) template, sheet);
            copySheetRow(template, sheet, sourceHeader, header);
            clearColumn(sheet, 0);
        }
        return sheet;
    }

    private void populateDetailsRowWithValues(Row tempRow, AddModSupLine detailLine, Map<String, Integer> keyMaps, CellStyle cellStyle) {
        IntStream.range(1, keyMaps.size()).forEach(cellNo -> tempRow.createCell(cellNo).setCellStyle(cellStyle));
        // ***** Replace line values which are always filled in *****
        fillCell(tempRow, keyMaps, "CHANGE", getTemplateActionName(detailLine.getChangeType()));
        fillCell(tempRow, keyMaps, "OFFICIAL DATE OF THE VERSION", detailLine.getVersionOfficialDate());
        fillCell(tempRow, keyMaps, "TYPE", detailLine.getRecordTypeNameForExport());
        fillCell(tempRow, keyMaps, "REGLISS REFERENCE", detailLine.getRecordReglissReference());
        fillCell(tempRow, keyMaps, "AKA ID", detailLine.getAkaReglissReference());
        fillCell(tempRow, keyMaps, "AKA QUALITY", detailLine.getQualityFromModElement());
        fillCell(tempRow, keyMaps, "RECORD ID", detailLine.getRecordId());
        fillCell(tempRow, keyMaps, "ELEMENT", detailLine.getModElementNoQuality());
        fillCell(tempRow, keyMaps, "FULL PRIMARY NAME", formatPrimaryNameForDetails(detailLine));
        // ***** Replace line values which are optional *****
        fillCell(tempRow, keyMaps, "OTHER NAME (AKA)", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.ALIAS_STRONG,
                AddModSupElementType.ALIAS_WEAK, AddModSupElementType.ALIAS_OTHER));
        fillCell(tempRow, keyMaps, "DOB", formatDateOfBirthForDetails(detailLine));
        fillCell(tempRow, keyMaps, "POB", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.PLACE_OF_BIRTH));
        fillCell(tempRow, keyMaps, "BIC CODE", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.BIC));
        fillCell(tempRow, keyMaps, "PASSPORT NUMBER", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.PASSPORT));
        fillCell(tempRow, keyMaps, "ISIN CODE", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.ISIN));
        fillCell(tempRow, keyMaps, "ADDRESS", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.ADDRESS));
        fillCell(tempRow, keyMaps, "CITIZENSHIP", getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.CITIZENSHIP));
    }

    private void populateSummaryRowWithValues(Map<String, String> summaryLine, Row tempRow, Map<String, Integer> keyMaps, CellStyle cellStyle) {
        IntStream.range(1, keyMaps.size()).forEach(cellNo -> tempRow.createCell(cellNo).setCellStyle(cellStyle));
        fillCell(tempRow, keyMaps, "Type", summaryLine.get("getRecordTypeNameForExport"));
        fillCell(tempRow, keyMaps, "REGLISS Reference / REGLISS AKA Reference", summaryLine.get("getRecordAliasReference"));
        fillCell(tempRow, keyMaps, "Primary name", summaryLine.get("getPrimaryName"));
        fillCell(tempRow, keyMaps, "Element", summaryLine.get("getModElement"));
        fillCell(tempRow, keyMaps, "New Value", summaryLine.get("getNewValue"));
        fillCell(tempRow, keyMaps, "Previous Value", summaryLine.get("getOldValue"));
    }

    private Map<String, Integer> getColumnsOrderMap(Row sourceRowOdd) {
        Map<String, Integer> keyMaps = new HashMap<>();
        for (int columnNumber = sourceRowOdd.getFirstCellNum(); columnNumber < sourceRowOdd.getLastCellNum(); columnNumber++) {
            String stringCellValue = sourceRowOdd.getCell(columnNumber).getStringCellValue().replaceAll("\\[|\\]", "");
            if (!stringCellValue.isEmpty()) keyMaps.put(stringCellValue, columnNumber);
        }
        return keyMaps;
    }

    private void copyHeadersDataFromTemplate(Sheet sheet, Sheet template, int tmpRowIndex) {
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i < 3; i++) {
            XSSFRow rowToCopy = (XSSFRow) template.getRow(tmpRowIndex - 1 + i);
            Row row = getRowInGivenSheet(sheet, lastRowNum + 2 + i);
            copySheetRow(template, sheet, rowToCopy, row);
        }
    }

    private Map<String, String> extractSummaryDataMap(AddModSupAction action, AddModSupLine summaryLine) {
        Map<String, String> map = new HashMap<>();
        map.put("getRecordTypeNameForExport", summaryLine.getRecordTypeNameForExport());
        map.put("getRecordAliasReference", summaryLine.getRecordAliasReference());
        map.put("getPrimaryName", summaryLine.getPrimaryName());
        map.put("getModElement", summaryLine.getModElement());
        map.put("getOldValue", (action == AddModSupAction.MOD || action == AddModSupAction.SUP) ? summaryLine.getOldValue() : "-");
        map.put("getNewValue", (action == AddModSupAction.ADD || action == AddModSupAction.MOD) ? summaryLine.getNewValue() : "-");
        return map;
    }

    private void fillSummaryCountTable(Sheet sheet, AddModSupCountLine count, String elementTypeName) {
        POIExtension.replaceFirstOccurenceOfTag(sheet, 0, "[ADD " + elementTypeName + "]", count.getAddCount());
        POIExtension.replaceFirstOccurenceOfTag(sheet, 0, "[MOD " + elementTypeName + "]", count.getModCount());
        POIExtension.replaceFirstOccurenceOfTag(sheet, 0, "[SUPP " + elementTypeName + "]", count.getSupCount());
        POIExtension.replaceFirstOccurenceOfTag(sheet, 0, "[TOTAL " + elementTypeName + "]", count.getTotalCount());
    }

    private void fillCell(Row tempRow, Map<String, Integer> keyMaps, String key, String value) {
        if (keyMaps.containsKey(key)) {
            tempRow.getCell(keyMaps.get(key)).setCellValue(value);
        }
    }

    private String formatDateOfBirthForDetails(AddModSupLine detailLine) {
        return getValueForOptionalDetailsFiled(detailLine, AddModSupElementType.DATE_OF_BIRTH)
                .replaceAll("[/-]", "");
    }

    private String formatPrimaryNameForDetails(AddModSupLine detailLine) {
        return detailLine.getPrimaryName()
                .replaceAll("[^a-zA-Z0-9]", " ")
                .toUpperCase();
    }

    private String getValueForOptionalDetailsFiled(AddModSupLine detailLine, AddModSupElementType... modElements) {
        if (asList(modElements).contains(detailLine.getModElementType())) {
            if (detailLine.getChangeType() == AddModSupAction.ADD || detailLine.getChangeType() == AddModSupAction.MOD) {
                return detailLine.getNewValue();
            }
            if (detailLine.getChangeType() == AddModSupAction.SUP) {
                return detailLine.getOldValue();
            }
        }
        return "";
    }

    private String getElementCountTagName(AddModSupCountLine count) {
        if (count.getElementType() == AddModSupCountType.TOTAL) {
            return count.getElementTypeName() + " of elements";
        }
        return count.getElementTypeName();
    }

    private String getTemplateActionName(AddModSupAction action) {
        switch (action) {
            case ADD:
                return "ADD";
            case MOD:
                return "MOD";
            case SUP:
                return "SUPP";
            default:
                throw new IllegalArgumentException("Not a valid action: " + action);
        }
    }

    private void cleanupTemplate(SXSSFWorkbook wb) {
        SheetsTypes.getList().forEach(sheet -> {
            wb.removeSheetAt(wb.getSheetIndex(sheet.getName()));
            wb.setSheetName(wb.getSheetIndex(sheet.getAux()), sheet.getName());
        });
    }

    private void copyTemplateToOutputFile(File template, File tempFile) {
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             Workbook workbook = new XSSFWorkbook()) {
            Files.copy(template.toPath(), fos);
            workbook.write(fos);
        } catch (IOException e) {
            log.error("Cannot generate ADD/MOD/SUPP file - cannot copy template at location {} : {}", tempFile.getAbsolutePath(),
                    e.getMessage());
        }
    }

    private void incrementBatchExecutionForFullPageWritten(AddModSupExportData addModSupExcelParams, ExportJobParameters exportJobParameters,
                                                           int numberOfWrittenLines) {
        if (numberOfWrittenLines % PAGE_SIZE == 0) {
            progressService.incrementBatchExportProgressForWritingPage(ExportPageType.RECORDS_FULLY_LOADED_PAGE, 1,
                    getTotalNumberOfExportedLines(addModSupExcelParams), PAGE_SIZE, exportJobParameters);
        }
    }

    private int getNumberOfSummaryLinesWrittenInPreviousDetailsTables(AddModSupExportData addModSupExcelParams, AddModSupAction action) {
        switch (action) {
            case ADD:
                return 0;
            case MOD:
                return addModSupExcelParams.getDetailsElementsCountForAction(AddModSupAction.ADD);
            case SUP:
                return addModSupExcelParams.getDetailsElementsCountForAction(AddModSupAction.ADD) +
                        addModSupExcelParams.getDetailsElementsCountForAction(AddModSupAction.MOD);
            default:
                throw new IllegalArgumentException("Not a valid action: " + action);
        }
    }

    public SheetRowData buildRowDataForDetailsSheet(Sheet template) {
        // Get row style and labels map
        Row sourceRowOdd = findFirstOccurenceOfCellInColumn(template, 0, 0, "ALL_ODD").getRow();
        CellStyle style = sourceRowOdd.getCell(1).getCellStyle();
        Map<String, Integer> keyMaps = getColumnsOrderMap(sourceRowOdd);
        return new SheetRowData(template, sourceRowOdd, keyMaps, style, style);
    }

    public SheetRowData buildRowDataForSummarySheet(Sheet template, String sectionName) {
        // Get row style and labels map
        Row sourceRowOdd = findFirstOccurenceOfCellInColumn(template, 0, 0, sectionName + "_ODD").getRow();
        Row sourceRowEven = findFirstOccurenceOfCellInColumn(template, 0, 0, sectionName + "_EVEN").getRow();
        CellStyle styleOdd = sourceRowOdd.getCell(1).getCellStyle();
        CellStyle styleEven = sourceRowEven.getCell(1).getCellStyle();
        Row sourceRowHeader = findFirstOccurenceOfCellInColumn(template, 0, 0, sectionName + "_HEADER").getRow();
        Map<String, Integer> keyMaps = getColumnsOrderMap(sourceRowHeader);
        return new SheetRowData(template, sourceRowHeader, keyMaps, styleEven, styleOdd);
    }

    public enum SheetsTypes {
        SUMMARY("SUMMARY"), DETAILS("DETAILS");

        private String name;

        SheetsTypes(String name) { this.name = name; }

        public static List<SheetsTypes> getList() { return asList(values()); }

        public String getName() { return this.name; }

        public String getPage(int sheetNo) { return this.name + "(" + sheetNo + ")"; }

        public String getAux() { return this.name + "_aux"; }

    }

    private class SheetRowData {
        private Map<String, Integer> keyMaps;
        private Sheet template;
        private Row rowHeader;
        private CellStyle styleOdd;
        private CellStyle styleEven;

        public SheetRowData(Sheet template, Row rowHeader, Map<String, Integer> keyMaps, CellStyle styleOdd, CellStyle styleEven) {
            this.template = template;
            this.rowHeader = rowHeader;
            this.keyMaps = keyMaps;
            this.styleOdd = styleOdd;
            this.styleEven = styleEven;
        }

        public Map<String, Integer> getKeyMaps() { return keyMaps; }

        public CellStyle getStyle() { return styleOdd; }

        public CellStyle getStyle(int currentRowNumber) { return currentRowNumber % 2 == 0 ? styleOdd : styleEven; }

        public Sheet getTemplate() { return template; }

        public Row getRowHeader() { return rowHeader; }
    }

}

