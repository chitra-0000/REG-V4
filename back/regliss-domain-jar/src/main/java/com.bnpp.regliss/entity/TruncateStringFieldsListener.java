package com.bnpp.regliss.entity;

import com.bnpp.regliss.utils.EntityUtils;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

public class TruncateStringFieldsListener {

    @PrePersist
    @PreUpdate
    public void truncateStringFields(AbstractSimpleEntity entity) { EntityUtils.truncateStringFields(entity); }
}
