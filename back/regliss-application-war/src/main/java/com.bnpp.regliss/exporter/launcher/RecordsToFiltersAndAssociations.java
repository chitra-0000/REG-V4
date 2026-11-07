package com.bnpp.regliss.exporter.launcher;

import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.RecordBucketContent;
import com.bnpp.regliss.vo.AssociationExternalReference;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RecordsToFiltersAndAssociations {

    private final Map<Long, RecordBucketContent> recordIdsToBuckets;

    public RecordsToFiltersAndAssociations() { recordIdsToBuckets = new HashMap<>(); }

    public RecordsToFiltersAndAssociations union(RecordsToFiltersAndAssociations other) {
        final Map<Long, RecordBucketContent> union = new HashMap<>(other.recordIdsToBuckets);
        recordIdsToBuckets.forEach((recordId, filterAssocs) -> union.merge(recordId, filterAssocs, this::unionFilterAssociations));
        return new RecordsToFiltersAndAssociations(union);
    }

    public void merge(Long recordId, RecordBucketContent filtersToAssociations) {
        recordIdsToBuckets.merge(recordId, filtersToAssociations, this::unionFilterAssociations);
    }

    public Set<Long> recordIds() { return recordIdsToBuckets.keySet(); }

    private RecordBucketContent unionFilterAssociations(RecordBucketContent fa1, RecordBucketContent fa2) {
        if(fa1.getRecordType() != fa2.getRecordType()){
            throw new IllegalArgumentException("Cannot merge two different record type buckets");
        }
        HashMap<Long, List<AssociationExternalReference>> union = new HashMap<>(fa1.getFiltersAndAssociations());
        union.putAll(fa2.getFiltersAndAssociations());
        return new RecordBucketContent(union, fa1.getRecordType(),fa1.getCreatedInVersion(),fa1.getClonedInVersion());
    }

    public Set<Long> getFilterIdsOfRecord(Long recordId) {
        return getBucketsForRecordId(recordId).getFiltersAndAssociations().keySet();
    }

    public RecordBucketContent getBucketsForRecordId(Long recordId) { return recordIdsToBuckets.get(recordId); }

    /*CPU intensive, use ONLY before starting to write Watchlist pages
    Because WL has more than 2 millions records this method could slow the export with great effect if used in a loop*/
    public RecordsToFiltersAndAssociations filteredRecordBucketsForRecordsTypes(List<RecordType> recordTypes) {
        Map<Long, RecordBucketContent> filteredRecordBucketsForRecordIdTypes = recordIdsToBuckets.entrySet().stream()
                .filter(e -> recordTypes.contains(e.getValue().getRecordType()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        return new RecordsToFiltersAndAssociations(filteredRecordBucketsForRecordIdTypes);
    }


    public Map<CounterKeys,Long> computeStatics(Long versionId,Long filterId){
        Map<CounterKeys,Long> result=new EnumMap<>(CounterKeys.class);
        result.put(CounterKeys.TOTAL,recordIdsToBuckets.entrySet().stream().filter(e->e.getValue().recordIsForFilter(filterId)).count());
        result.put(CounterKeys.TOTALADD,recordIdsToBuckets.entrySet().stream().filter(e->e.getValue().recordIsForFilter(filterId))
                .filter(e -> e.getValue().isAdded(versionId)).count());
        result.put(CounterKeys.TOTALMOD,recordIdsToBuckets.entrySet().stream().filter(e->e.getValue().recordIsForFilter(filterId))
                .filter(e -> e.getValue().isModified(versionId)).count());
        result.put(CounterKeys.PERSONADD,recordIdsToBuckets.entrySet().stream().filter(e->e.getValue().recordIsForFilter(filterId))
                .filter(e -> e.getValue().isPersonAdded(versionId)).count());
        result.put(CounterKeys.ENTITYADD,result.get(CounterKeys.TOTALADD)-result.get(CounterKeys.PERSONADD));
        result.put(CounterKeys.PERSONMOD,recordIdsToBuckets.entrySet().stream().filter(e->e.getValue().recordIsForFilter(filterId))
                .filter(e -> e.getValue().isPersonModified(versionId)).count());
        result.put(CounterKeys.ENTITYMOD,result.get(CounterKeys.TOTALMOD)-result.get(CounterKeys.PERSONMOD));
        return result;

    }
}
