package com.bnpp.regliss.exporter.writer.format;

// [MISSING FROM SCREENSHOTS: lines 3-5 of the original file were not visible in the photos]

public abstract class ExportFormatWriter {
    protected final GeneratedFileWriter fileWriter;

    protected ExportFormatWriter(GeneratedFileWriter fileWriter) { this.fileWriter = fileWriter; }

    public abstract void writeElement(ExportPageType pageType, ExportElementData dataForRecord, FileExportData fileExportData);

    public GeneratedFileWriter getGeneratedFileWriter() { return fileWriter; }
}

