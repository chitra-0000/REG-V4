package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.batch.service.BatchProgressService;
import com.bnpp.regliss.batch.service.ExportJobParameters;
import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.exporter.writer.format.ExportFormatWriter;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.monitoring.ProgressMonitor;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.vo.export.ExportElementData;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ReglissBatchProfile
public abstract class ExportPagedWriter {

    // Warning: NOT exposed as .properties config due to: OutOfMemory for larger sizes + Oracle Limitation <1000 IN
    private static final int RECORD_PAGE_SIZE = 500;

    @Autowired
    private BatchProgressService progressService;

    @Autowired
    private ExportPageLoader exportPageLoader;

    public void writeInPagesInAllFiles(ExportPageType pageType, FileExportData fileExportData, List<? extends
        ExportFormatWriter> pagedFormatWriters, ExportJobParameters exportJobParameters) {
        ProgressMonitor progressMonitor = new ProgressMonitor(fileExportData.getExportType(), log);
        if (!pagedFormatWriters.isEmpty()) {
            if(fileExportData.hasNoRecordsToExport()){
                progressService.incrementBatchExportProgressForWritingAllPagesAtOnceOrNoPages(pageType.getExportPhase(),
                        pagedFormatWriters.size(), exportJobParameters);
            }

            for (List<Long> idsPartition : Iterables.partition(fileExportData.getRecordsIds(), RECORD_PAGE_SIZE)) {
                Map<Long, ExportElementData> recordPage = loadPage(pageType, fileExportData, progressMonitor, idsPartition);

                writePage(recordPage, idsPartition, pageType, pagedFormatWriters, fileExportData);
                progressService.incrementBatchExportProgressForWritingPage(pageType, pagedFormatWriters.size(),
                        fileExportData.getTotalNumberOfRecords(), RECORD_PAGE_SIZE, exportJobParameters);
            }
        }
        progressMonitor.end();
    }

    private Map<Long, ExportElementData> loadPage(ExportPageType pageType, FileExportData fileExportData, ProgressMonitor progressMonitor,
                                                  List<Long> idsPartition) {
        log.trace("Load a record page: START");
        Map<Long, ExportElementData> recordPage = exportPageLoader.loadPage(pageType, idsPartition, fileExportData);
        log.trace("Load a record page: END");

        progressMonitor.add(idsPartition.size());
        return recordPage;
    }

    private void writePage(Map<Long, ExportElementData> loadedRecordPage, List<Long> idsPartition, ExportPageType pageType,
                           List<? extends ExportFormatWriter> pagedFormatWriters, FileExportData fileExportData) {
        for (Long recordId : idsPartition) {
            for (ExportFormatWriter wr : fileExportData.getWritersForRecord(recordId, pagedFormatWriters)) {
                ExportElementData elementData = loadedRecordPage.get(recordId);
                if (elementData != null) { wr.writeElement(pageType, elementData, fileExportData); }
            }
        }
    }
}
