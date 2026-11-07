package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.batch.service.ExportPhase;
import com.bnpp.regliss.entity.referential.FileExportExtension;
import com.bnpp.regliss.exporter.writer.ExportPagedWriter;
import com.bnpp.regliss.exporter.writer.GeneratedFileWriter;
import com.bnpp.regliss.monitoring.ProgressMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@ReglissBatchProfile
public class DJExportWriter extends ExportPagedWriter {

    @Autowired
    private DJExportFormatFactory formatFactory;

    public void generateAllDjFiles(DJFileExportData djRecordsExportData,
                                   List<GeneratedFileWriter> fileWriters,
                                   ExportJobParameters exportJobParameters) {
        ProgressMonitor progressMonitor = new ProgressMonitor("Export Type " + djRecordsExportData.getExportType(), log);

        List<GeneratedFileWriter> xmlFileWriters = fileWriters.stream().filter(w -> FileExportExtension.xml == w.getFileFormat()
                        .getFileExtension()
                || FileExportExtension.zip == w.getFileFormat().getFileExtension())
                .collect(toList());
        writeXmlFiles(djRecordsExportData, exportJobParameters, xmlFileWriters);

        List<GeneratedFileWriter> csvFileWriters = fileWriters.stream().filter(w -> FileExportExtension.csv == w.getFileFormat()
                        .getFileExtension())
                .collect(toList());
        if(!csvFileWriters.isEmpty()) {
            writeWatchlistAmeCsvFiles((DJWatchlistAmeFileExportData) djRecordsExportData, exportJobParameters, csvFileWriters);
        }

        progressMonitor.end();
    }

    private void writeXmlFiles(DJFileExportData djFileExportData, ExportJobParameters exportJobParameters,
                               List<GeneratedFileWriter> xmlFileWriters) {
        List<DJXmlExportFormatWriter> exportFormatWriters = formatFactory.createAllXmlFormats(xmlFileWriters);

        for (DJXmlExportFormatWriter format : exportFormatWriters) {
            format.writeHeader(djFileExportData);
            format.startRecords();
        }

        writeElementsForPhase(ExportPhase.EXPORT_WRITE_RECORDS, djFileExportData, exportFormatWriters, exportJobParameters);

        for (DJXmlExportFormatWriter format : exportFormatWriters) {
            format.endRecords();
            format.startAssociations();
        }

        writeElementsForPhase(ExportPhase.EXPORT_WRITE_ASSOCIATIONS, djFileExportData, exportFormatWriters, exportJobParameters);

        for (DJXmlExportFormatWriter format : exportFormatWriters) {
            format.endAssociations();
            format.writeFooter();
        }
    }

    private void writeElementsForPhase(ExportPhase phase, DJFileExportData djFileExportData, List<DJXmlExportFormatWriter> exportFormatWriters,
                                       ExportJobParameters exportJobParameters) {
        for (DJRecordTagName recordDjStartingTagName : DJRecordTagName.getOrderedRecordTagNamesForExport()) {
            ExportPageType pageType = ExportPageType.getDJPageTypeByPhaseAndExtension(phase, FileExportExtension.xml);
            DJFileExportData exportDataForRecordTypes = djFileExportData.getExportDataForRecordTypes(recordDjStartingTagName
                    .getCorrespondingRecordTypes());
            writeInPagesInAllFiles(pageType, exportDataForRecordTypes, exportFormatWriters, exportJobParameters);
        }
    }

    private void writeWatchlistAmeCsvFiles(DJWatchlistAmeFileExportData djFileExportData, ExportJobParameters exportJobParameters,
                                           List<GeneratedFileWriter> xmlFileWriters) {
        List<WatchlistAmeCsvExportFormatWriter> djFormats = formatFactory.createAllCsvFormats(xmlFileWriters);

        for(DJRecordTagName recordDjStartingTagName : DJRecordTagName.getOrderedRecordTagNamesForExport()) {
            ExportPageType pageType = ExportPageType.getDJPageTypeByPhaseAndExtension(ExportPhase.EXPORT_WRITE_RECORDS, FileExportExtension.csv);
            DJFileExportData exportDataForRecordTypes = djFileExportData.getExportDataForRecordTypes(recordDjStartingTagName
                    .getCorrespondingRecordTypes());
            writeInPagesInAllFiles(pageType, exportDataForRecordTypes, djFormats, exportJobParameters);
        }
    }

}
