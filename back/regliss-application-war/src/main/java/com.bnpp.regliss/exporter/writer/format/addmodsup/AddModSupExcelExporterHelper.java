package com.bnpp.regliss.exporter.writer.format.addmodsup;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.POIXMLDocumentPart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChart;
import org.openxmlformats.schemas.drawingml.x2006.chart.CTChartSpace;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Valcu Dacian (8/19/2020)
 */
@Slf4j
public class AddModSupExcelExporterHelper {

    private AddModSupExcelExporterHelper() {
    }

    public static void migrateXSSSheetDataToAuxiliarNewSXSSSheet(SXSSFWorkbook workbook, List<AddModSupExcelExporter.SheetsTypes> sheetsTypes) {
        // Get internal old workbook
        XSSFWorkbook oldWorkbook = workbook.getXSSFWorkbook();
        // Copy sheets
        sheetsTypes.stream().forEach(sheetName -> {
            XSSFSheet oldSheet = oldWorkbook.getSheet(sheetName.getName());
            SXSSFSheet newSheet = (SXSSFSheet) workbook.createSheet(sheetName.getAux());

            // Copy default style
            copySheetDefaultStyle(oldSheet, newSheet);

            // Copy charts
            copySheetDrawnings(oldSheet, oldWorkbook.getSheet(sheetName.getAux()));

            // Copy content
            copySheetContent(oldSheet, newSheet);

            // Copy merged regions
            copyMergedRegions(oldSheet, newSheet);
        });

    }

    private static void copySheetDefaultStyle(XSSFSheet oldSheet, SXSSFSheet newSheet) {
        newSheet.setDefaultRowHeight(oldSheet.getDefaultRowHeight());
        newSheet.setDefaultColumnWidth(oldSheet.getDefaultColumnWidth());
        IntStream.range(0, 10).forEach(i->
                newSheet.setDefaultColumnStyle(i, oldSheet.getColumnStyle(i))
        );
    }

    private static void copySheetContent(XSSFSheet oldSheet, SXSSFSheet newSheet) {
        for (int rowNumber = oldSheet.getFirstRowNum(); rowNumber < oldSheet.getLastRowNum(); rowNumber++) {
            final XSSFRow oldRow = oldSheet.getRow(rowNumber);
            if (oldRow != null) {
                Row newRow = newSheet.createRow(rowNumber);
                newRow.setHeight(oldRow.getHeight());

                copySheetRow(oldSheet, newSheet, oldRow, newRow);
            }
        }
    }

    private static void copySheetDrawnings(XSSFSheet oldSheet, XSSFSheet newSheet) {
        List<POIXMLDocumentPart> rels = oldSheet.getRelations();
        // if the sheet being cloned has a drawing then remember it and re-create tpoo
        XSSFDrawing dg = getXssfDrawing(newSheet, rels);
        CTWorksheet ct = newSheet.getCTWorksheet();
        // clone the sheet drawing alongs with its relationships
        if (dg != null) {
            if (ct.isSetDrawing()) {
                // unset the existing reference to the drawing,
                ct.unsetDrawing();
            }
            XSSFDrawing clonedDg = newSheet.createDrawingPatriarch();
            // copy drawing contents
            clonedDg.getCTDrawing().set(dg.getCTDrawing());
            // clone drawing relations
            List<POIXMLDocumentPart> srcRels = oldSheet.createDrawingPatriarch().getRelations();
            for (POIXMLDocumentPart rel : srcRels) {
                if (rel instanceof XSSFChart) {
                    copyDrawingChart(clonedDg, (XSSFChart) rel);
                } else {
                    PackageRelationship relation = rel.getPackageRelationship();
                    newSheet.createDrawingPatriarch().getPackagePart().addRelationship(
                            relation.getTargetURI(), relation.getTargetMode(), relation.getRelationshipType(), relation.getId());
                }
            }
        }
    }

    private static XSSFDrawing getXssfDrawing(XSSFSheet newSheet, List<POIXMLDocumentPart> rels) {
        XSSFDrawing dg = null;
        for (POIXMLDocumentPart r : rels) {
            // do not copy the drawing relationship, it will be re-created
            if (r instanceof XSSFDrawing) {
                dg = (XSSFDrawing) r;
                continue;
            }
            PackageRelationship rel = r.getPackageRelationship();
            newSheet.getPackagePart().addRelationship(
                    rel.getTargetURI(), rel.getTargetMode(),
                    rel.getRelationshipType());
            newSheet.addRelation(rel.getId(), r);
        }
        return dg;
    }

    private static void copyDrawingChart(XSSFDrawing clonedDg, XSSFChart chart) {
        try {
            // create new chart
            int chartNumber = clonedDg.getPackagePart().getPackage().getPartsByContentType(XSSFRelation.CHART.getContentType()).size() + 1;
            XSSFChart c = (XSSFChart) clonedDg.createRelationship(XSSFRelation.CHART, XSSFFactory.getInstance(), chartNumber);

            // duplicate source CTChart
            CTChart ctc = (CTChart) chart.getCTChart().copy();
            c.getCTChart().set(ctc);

            // duplicate source CTChartSpace
            CTChartSpace ctcs = (CTChartSpace) chart.getCTChartSpace().copy();
            c.getCTChartSpace().set(ctcs);

            // create new relation for the new chart
            PackageRelationship relation = c.getPackageRelationship();
            clonedDg.getPackagePart().addRelationship(
                    relation.getTargetURI(), relation.getTargetMode(), relation.getRelationshipType(), relation.getId());
        } catch (Exception e) {
            log.error("Cannot copy the report ADD/MOD/SUPP drawnings: {}", e.getMessage());
        }
    }

    private static void copyMergedRegions(Sheet oldSheet, Sheet newSheet) {
        for (int i = 0; i < oldSheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = oldSheet.getMergedRegion(i);
            newSheet.addMergedRegion(
                    new CellRangeAddress(region.getFirstRow(), region.getLastRow(), region.getFirstColumn(), region.getLastColumn()));
        }

    }

    public static void copySheetRow(Sheet oldSheet, Sheet newSheet, XSSFRow oldRow, Row newRow) {
        for (int columnNumber = oldRow.getFirstCellNum(); columnNumber < oldRow.getLastCellNum(); columnNumber++) {
            newSheet.setColumnWidth(columnNumber, oldSheet.getColumnWidth(columnNumber));
            XSSFCell oldCell = oldRow.getCell(columnNumber);
            if (oldCell != null) {
                Cell newCell = newRow.createCell(columnNumber);
                // Copy value
                newCell.setCellValue(oldCell.getStringCellValue());
                // Copy style
                CellStyle newCellStyle = newSheet.getWorkbook().createCellStyle();
                newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
                newCell.setCellStyle(newCellStyle);
            }
        }
    }
}

