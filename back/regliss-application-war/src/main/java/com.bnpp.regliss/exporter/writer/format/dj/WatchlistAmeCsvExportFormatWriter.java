package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.exporter.writer.format.ExportFormatWriter;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ReglissBatchProfile
public class WatchlistAmeCsvExportFormatWriter extends ExportFormatWriter {

    public WatchlistAmeCsvExportFormatWriter(GeneratedFileWriter writer) { super(writer); }

    @Override
    public void writeElement(ExportPageType pageType, ExportElementData dataForRecord, FileExportData fileExportData) {
        if (pageType == ExportPageType.RECORDS_CSV_SNIPPETS_PAGE) {
            writeRecordSnippet((DJCsvRecordExportElementData) dataForRecord);
        } else {
            throw new IllegalArgumentException("Page type not supported for DJ XML export: " + pageType);
        }
    }

    private void writeRecordSnippet(DJCsvRecordExportElementData djCsvRecordExportElementData) {
        RecordDjExportData r = djCsvRecordExportElementData.getRecordDjExportData();
        writeCsvSnippet(r.getExportSnippet(), r.getUrlReference());
    }


    private void writeCsvSnippet(String csvSnippet, String externalReference) {
        if (csvSnippet == null) {
            log.warn("No CSV snippet to export for record externalReference={}", externalReference);
            fileWriter.writeLine("");
        } else {

            fileWriter.writeLine(csvSnippet );
        }
    }

}
