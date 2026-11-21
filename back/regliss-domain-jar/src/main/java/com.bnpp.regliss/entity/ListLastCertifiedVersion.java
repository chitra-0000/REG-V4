package com.bnpp.regliss.entity;

import lombok.Data;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;

@IdClass(ListLastCertifiedVersion.ListLastCertifiedVersionId.class)
@Entity
@Table(name="LIST_LAST_CERTIFIED_VERSION")
public class ListLastCertifiedVersion {

    @Data
    public static class ListLastCertifiedVersionId implements Serializable{
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
