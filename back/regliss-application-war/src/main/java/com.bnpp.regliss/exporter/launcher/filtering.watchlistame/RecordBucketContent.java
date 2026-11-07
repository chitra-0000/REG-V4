package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.vo.AssociationExternalReference;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class RecordBucketContent {
    private Map<Long, List<AssociationExternalReference>> filtersAndAssociations;
    private RecordType recordType;
    private Long createdInVersion;
    private Long clonedInVersion;

    public boolean isPerson() { return recordType.equals(RecordType.PERSON); }
    public boolean isAdded(Long versionId) { return (createdInVersion!=null && createdInVersion.equals(versionId)); }
    public boolean isModified(Long versionId) { return (clonedInVersion!=null && clonedInVersion.equals(versionId)); }
    public boolean isPersonAdded(Long versionId) { return this.isPerson() && this.isAdded(versionId); }
    public boolean isPersonModified(Long versionId) { return this.isPerson() && this.isModified(versionId); }
    public boolean recordIsForFilter(Long filterId){return filtersAndAssociations.get(filterId)!=null;}
}

