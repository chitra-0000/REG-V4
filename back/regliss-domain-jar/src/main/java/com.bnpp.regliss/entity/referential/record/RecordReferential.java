package com.bnpp.regliss.entity.referential.record;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Optional;

@MappedSuperclass
public abstract class RecordReferential extends AbstractSimpleEntity implements ToStringAuditable {

    @Getter
    @Setter
    @EqualityField
    @Column(name = "BUSINESS_CONCEPT")
    @Enumerated(EnumType.STRING)
    protected ReferentialBusinessConcept businessConcept;
    @Getter
    @Setter
    @Column(name = "V3_ONLY")
    protected boolean v3Only;
    @Getter
    @Setter
    @EqualityField
    @Column(name = "LABEL")
    private String label;

    public RecordReferential() {
    }

    public RecordReferential(String label) { this.label = label; }

    public RecordReferential(ReferentialBusinessConcept businessConcept) { this.businessConcept = businessConcept; }

    public RecordReferential(String label, ReferentialBusinessConcept businessConcept) {
        this.label = label;
        this.businessConcept = businessConcept;
    }

    public RecordReferential(RecordReferential other) {
        this(other.label, other.businessConcept);
        this.setId(other.getId());
    }

    public Optional<ReferentialBusinessConcept> getBusinessConceptOpt() { return Optional.ofNullable(businessConcept); }

    public boolean hasBusinessConcept(ReferentialBusinessConcept businessConcept){
        return this.businessConcept == businessConcept;
    }

    @Override
    public String toStringAudit() { return this.getLabel(); }
}
