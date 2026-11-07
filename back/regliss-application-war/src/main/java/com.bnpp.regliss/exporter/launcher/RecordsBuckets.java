package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import com.bnpp.regliss.vo.AssociationExternalReference;
import com.bnpp.regliss.vo.export.dj.watchlist.FilterIdTag;
import com.bnpp.regliss.vo.export.dj.watchlist.RecordDjExportData;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class RecordsBuckets {
    private final RecordsToFiltersAndAssociations filteredRecordBuckets;
    @Getter private final List<FilterIdTag> filterTags;

    public RecordsBuckets(RecordsToFiltersAndAssociations filteredRecordBuckets, List<FilterIdTag> filterTags) {
        this.filteredRecordBuckets = filteredRecordBuckets;
        this.filterTags = filterTags;
    }

    public List<String> getDescription2IdsOfIncludedExcludedFilterTagsForRecord(RecordDjExportData recordDjExportData,
            Map<RecordFilterInclusionTag, Integer> filterTagsToDescription2Ids) {
        Set<Long> filterIdsOfRecord = getFilterIdsOfRecord(recordDjExportData.getId());
        return getIncludedExcludedTagsAsDescription2Ids(filterIdsOfRecord, filterTagsToDescription2Ids);
    }

    public Set<Long> getFilterIdsOfRecord(Long id) { return filteredRecordBuckets.getFilterIdsOfRecord(id); }

    private List<String> getIncludedExcludedTagsAsDescription2Ids(Set<Long> includedFilterIds, Map<RecordFilterInclusionTag,
            Integer> filterTagsToDescription2Ids) {
        return filterTags.stream()
                .map(ft -> toFilterInclusionTag(ft, includedFilterIds))
                .map(filterTagsToDescription2Ids::get)
                .map(String::valueOf)
                .collect(toList());
    }

    public List<AssociationExternalReference> associationsForRecordAndFilter(Long recordId, Long filterId) {
        return filteredRecordBuckets.getBucketsForRecordId(recordId).getFiltersAndAssociations().get(filterId);
    }

    public List<String> getIncludedExcludedFilterTagsForRecord(Long recordId) {
        Set<Long> filterIdsOfRecord = getFilterIdsOfRecord(recordId);
        return getIncludedExcludedTags(filterIdsOfRecord);
    }

    public Set<Long> recordIds() { return filteredRecordBuckets.recordIds(); }

    private List<String> getIncludedExcludedTags(Set<Long> includedFilterIds) {
        return filterTags.stream()
                .map(ft -> toFilterInclusionTag(ft, includedFilterIds))
                .map(RecordFilterInclusionTag::getExportValue)
                .collect(toList());
    }

    private RecordFilterInclusionTag toFilterInclusionTag(FilterIdTag filterIdTag, Set<Long> includedFilterIds) {
        return includedFilterIds.contains(filterIdTag.getId()) ?
                RecordFilterInclusionTag.includedFilterTag(filterIdTag.getFilterTag()) :
                RecordFilterInclusionTag.excludedFilterTag(filterIdTag.getFilterTag());
    }

    public RecordsBuckets bucketsForRecordsTypes(List<RecordType> recordTypes) {
        RecordsToFiltersAndAssociations recordsToFiltersAndAssociations =
                filteredRecordBuckets.filteredRecordBucketsForRecordsTypes(recordTypes);
        return new RecordsBuckets(recordsToFiltersAndAssociations, filterTags);
    }

    public Map<CounterKeys,Long> computeStatistics(Long versionId,Long filterId){
        return this.filteredRecordBuckets.computeStatics(versionId,filterId);
    }
}
