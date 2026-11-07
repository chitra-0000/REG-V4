package com.bnpp.regliss.exporter.writer.format.dj;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.launcher.RecordFilterInclusionTag;
import com.bnpp.regliss.exporter.launcher.RecordsBuckets;
import com.bnpp.regliss.exporter.launcher.WatchlistAmeOriginalCategoryIds;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.WatchlistAmeFilterTagsToDescription2Details;
import com.bnpp.regliss.exporter.writer.format.ExportFormatWriter;
import com.bnpp.regliss.vo.AssociationExternalReference;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.EqualsAndHashCode;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = true)
public class DJWatchlistAmeFileExportData extends DJFileExportData {

    private final RecordsBuckets recordsBuckets;
    private final WatchlistAmeFilterTagsToDescription2Details watchlistAmeFilterTagsToDescription2Details;
    private final WatchlistAmeOriginalCategoryIds categoryIds;

    public DJWatchlistAmeFileExportData(RecordsBuckets recordsBuckets,
       WatchlistAmeFilterTagsToDescription2Details watchlistAmeFilterTagsToDescription2Details, Version version,
                                        WatchlistAmeOriginalCategoryIds categoryIds) {
        super(version);
        this.recordsBuckets = recordsBuckets;
        this.watchlistAmeFilterTagsToDescription2Details = watchlistAmeFilterTagsToDescription2Details;
        this.categoryIds = categoryIds;
    }

    @Override
    public Set<Long> getRecordsIds() { return recordsBuckets.recordIds(); }

    @Override
    public String getExportType() { return "Watchlist export, all filters" ; }

    @Override
    public List<? extends ExportFormatWriter> getWritersForRecord(Long recordId, List<? extends ExportFormatWriter> pagedFormatWriters) {
        Set<Long> filterIdsOfRecord = recordsBuckets.getFilterIdsOfRecord(recordId);
        return pagedFormatWriters.stream()
            .filter(wr -> filterIdsOfRecord.contains(getFilterId(wr)))
            .collect(Collectors.toList());
    }


    @Override
    public Map<ExportXmlTagType, List<String>> getXmlAdditionalExportTagsValuesForRecord(RecordDjExportData recordDjExportData) {
         List<String> includedExcludedFilterTagsMapForPartition = recordsBuckets
                 .getDescription2IdsOfIncludedExcludedFilterTagsForRecord(recordDjExportData, getFilterTagsToDescription2Ids());

        Map<ExportXmlTagType, List<String>> additionalTagsForRecord = new EnumMap<>(ExportXmlTagType.class);
        additionalTagsForRecord.put(ExportXmlTagType.FILTER_INCLUSION, includedExcludedFilterTagsMapForPartition);
        additionalTagsForRecord.put(ExportXmlTagType.WL_DESCRIPTION2_OTHER, singletonList(getFilterTagsToDescription2Ids()
                .get(RecordFilterInclusionTag.OTHER).toString()));
        additionalTagsForRecord.put(ExportXmlTagType.WL_DESCRIPTION1_MIN_ID,

                singletonList(Integer.toString(watchlistAmeFilterTagsToDescription2Details.getMinDescription1Id())));
        additionalTagsForRecord.put(ExportXmlTagType.SOURCE, singletonList(recordDjExportData.getUrlReference()));
        return additionalTagsForRecord;
    }

    @Override
    public DJFileExportData getExportDataForRecordTypes(List<RecordType> correspondingRecordTypes) {
        RecordsBuckets recordBucketsForTypes = recordsBuckets.bucketsForRecordsTypes(correspondingRecordTypes);
        return new DJWatchlistAmeFileExportData(recordBucketsForTypes, watchlistAmeFilterTagsToDescription2Details,
                getVersion(), categoryIds);
    }

    public List<String> getCsvAdditionalExportTagsValuesForRecord(Long recordId) {
        return recordsBuckets.getIncludedExcludedFilterTagsForRecord(recordId);
    }


    private Long getFilterId(ExportFormatWriter wr) {
        return wr.getGeneratedFileWriter().getFileFormat().getFilter().getId();
    }

    private Map<RecordFilterInclusionTag, Integer> getFilterTagsToDescription2Ids() {
        return watchlistAmeFilterTagsToDescription2Details.getFilterTagsToDescription2Ids();
    }


    public String getEnrichedReferentialXmlPrefix() {
        return watchlistAmeFilterTagsToDescription2Details.getEnrichedReferentialXmlPrefix();
    }


    public List<AssociationExternalReference> getAssociationsForRecordAndFilter(Long recordId, Long filterId) {
        return recordsBuckets.associationsForRecordAndFilter(recordId, filterId);
    }

    public boolean atLeastOneOfIdsIsPepOrRca(Set<String> description1Ids) {
        return categoryIds.getPepOrRcaCategoryIds().stream()
                .anyMatch(description1Ids::contains);
    }

    @Override
    public Map<CounterKeys, Long> computeStatistics(Long filterId) {
        return this.recordsBuckets.computeStatistics(this.getVersion().getId(),filterId);
    }
}
