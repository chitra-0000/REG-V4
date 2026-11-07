package com.bnpp.regliss.exporter.writer;

import com.bnpp.regliss.batch.service.ExportPageType;
import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.exporter.writer.format.FileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.DJCsvRecordExportElementData;
import com.bnpp.regliss.exporter.writer.format.dj.DJFileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.DJWatchlistAmeFileExportData;
import com.bnpp.regliss.exporter.writer.format.dj.DJXmlRecordExportElementData;
import com.bnpp.regliss.exporter.writer.format.regular.RegularExportElementData;
import com.bnpp.regliss.exporter.writer.format.regular.RegularFileExportData;
import com.bnpp.regliss.importer.RecordChildrenFetcher;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.RecordRepository;
import com.bnpp.regliss.vo.export.ExportElementData;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@ReglissBatchProfile
public class ExportPageLoader {

    @Autowired
    private RecordRepository recordRepo;

    @Autowired
    private RecordChildrenFetcher recordChildrenFetcher;

    public Map<Long, ExportElementData> loadPage(ExportPageType pageType, List<Long> idsPartition, FileExportData fileExportData){
        switch (pageType) {
            case RECORDS_FULLY_LOADED_PAGE:
                return extractFullRecords(idsPartition, (RegularFileExportData) fileExportData);
            case RECORDS_XML_SNIPPETS_PAGE:
                return getDjXmlRecordsExportPageData(idsPartition, (DJFileExportData) fileExportData);
            case RECORDS_CSV_SNIPPETS_PAGE:
                return getDjCsvRecordsExportPageData(idsPartition, (DJWatchlistAmeFileExportData) fileExportData);
            case ASSOCIATIONS_XML_SNIPPETS_PAGE:
                return getDjAssociationsExportPageData(idsPartition);
            default :
                throw new IllegalArgumentException("Illegal page type for export: " + pageType);
        }
    }

    private Map<Long, ExportElementData> extractFullRecords(List<Long> idsPartition, RegularFileExportData fileExportData) {
        log.trace("Load a record page: START");
        List<Record> bareRecordsInPage = recordRepo.findAllById(idsPartition);
        List<Record> fullyLoadedRecords = recordChildrenFetcher.prefetchChildrenForRecordPage(bareRecordsInPage);
        log.trace("Load a record page: END");
        return fullyLoadedRecords.stream()
                .collect(
                        toMap(Record::getId, r -> new RegularExportElementData(r, fileExportData.getVersionForRecord(r.getId())))
                );
    }

    private Map<Long, ExportElementData> getDjXmlRecordsExportPageData(List<Long> recordIdsPartition, DJFileExportData fileExportData) {
        List<RecordDjExportData> recordDjExportDataPartition = recordRepo.getDjXmlExportDataInPartitionByConfidentiality(recordIdsPartition,
                fileExportData.getVersion().getList());

        return recordDjExportDataPartition.stream()
            .collect(toMap(
                RecordDjExportData::getId,
                rdj -> new DJXmlRecordExportElementData(rdj, fileExportData.getXmlAdditionalExportTagsValuesForRecord(rdj))
            ));
    }

    private Map<Long, ExportElementData> getDjCsvRecordsExportPageData(List<Long> recordIdsPartition,
                                                                       DJWatchlistAmeFileExportData fileExportData) {
        List<RecordDjExportData> recordDjExportDataPartition = recordRepo.getDjCsvExportDataInPartitionByRecordTypesAndConfidentiality(
                recordIdsPartition, fileExportData.getVersion().getList());
        return recordDjExportDataPartition.stream()
            .collect(toMap(
                RecordDjExportData::getId,
                rdj -> new DJCsvRecordExportElementData(rdj)
            ));
    }

    private Map<Long, ExportElementData> getDjAssociationsExportPageData(List<Long> recordIdsPartition) {
        return recordRepo.getAssociationXmlSnippetsInPartitionByRecordTypes(recordIdsPartition);
    }

}
