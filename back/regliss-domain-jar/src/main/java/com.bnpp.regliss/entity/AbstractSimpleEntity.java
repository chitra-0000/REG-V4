package com.bnpp.regliss.entity;

import com.bnpp.regliss.utils.AuditUtil;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(TruncateStringFieldsListener.class)
public abstract class AbstractSimpleEntity extends AbstractEntity<Long> {

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{id=" + getId() + ",instance=" + System.identityHashCode(this) + "}";
    }

    public String toStringAudit() { return AuditUtil.auditedFieldsToString(this); }
}
