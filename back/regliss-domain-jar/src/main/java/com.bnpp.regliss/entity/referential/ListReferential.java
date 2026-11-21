package com.bnpp.regliss.entity.referential;

import com.bnpp.regliss.annotation.EqualityField;
import com.bnpp.regliss.entity.AbstractSimpleEntity;
import com.bnpp.regliss.entity.record.ToStringAuditable;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ListReferential extends AbstractSimpleEntity implements ToStringAuditable{

    @Getter @Setter
    @EqualityField
    @Column(name = "LABEL_EN")
    private String label;

    public ListReferential() {
    }

    public ListReferential(String label) { this.label = label; }

    public ListReferential(ListReferential other) {
        this(other.getLabel());
        this.setId(other.getId());
    }

    @Override
    public String toStringAudit() { return String.valueOf(this.getId()); }
}
