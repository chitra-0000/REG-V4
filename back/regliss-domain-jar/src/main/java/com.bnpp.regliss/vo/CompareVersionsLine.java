package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.entity.referential.record.RecordType;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class CompareVersionsLine {
    private final RecordType recordType;
    private final String fullName;
    private final String reglissReference;
    private final String id;
    public String fromVersionNumber;
    private final long fromVersionId;
    public String toVersionNumber;
    private final long toVersionId;
    private final String deletedInVersionId;

    public CompareVersionsLine(Record r){
        this.recordType = r.getType();
        this.fullName = r.getFullName();
        this.reglissReference = r.getReglissReference();
        this.id = r.getExternalUuid();
        this.fromVersionId = r.getVersionLinks().getFromVersionId();
        this.toVersionId = r.getVersionLinks().getToVersionId();
        this.deletedInVersionId = r.getVersionLinks().getDeletedInVersionId() != null ? r.getVersionLinks().getDeletedInVersionId().toString() : null;
    }

    public long getFromVersionId() {
        return fromVersionId;
    }

    public long getToVersionId() {
        return toVersionId;
    }
}
