package com.bnpp.regliss.entity;

import lombok.Data;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;

@IdClass(ListInProgressVersion.ListInProgressVersionId.class)
@Entity
@Table(name="LIST_IN_PROGRESS_VERSION")
public class ListInProgressVersion {

    @Data
    public static class ListInProgressVersionId implements Serializable{
        private long listId;
        private long versionId;
    }

    @Id
    @Column(name="LIST_ID")
    private long listId;

    @Id
    @Column(name="VERSION_ID")
    private long versionId;

}
