package com.bnpp.regliss.entity.record;

import com.bnpp.regliss.entity.Version;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class RecordVersionLinks {

    @Getter
    @Column(name = "CREATED_IN_VERSION_ID")
    private Long createdInVersionId;

    @Getter
    @Column(name = "CLONED_IN_VERSION_ID")
    private Long clonedInVersionId;

    @Getter
    @Column(name = "FROM_VERSION")
    private Long fromVersionId;

    @Getter
    @Column(name = "TO_VERSION")
    private Long toVersionId;

    @Getter
    @Column(name = "LIST_ID")
    private Long listId;

    @Getter
    @Column(name = "DELETED_IN_VERSION")
    private Long deletedInVersionId;

    public RecordVersionLinks() {}

    public RecordVersionLinks(RecordVersionLinks other) {
        this.createdInVersionId = other.createdInVersionId;
        this.fromVersionId = other.fromVersionId;
        this.listId = other.listId;
        this.clonedInVersionId = other.clonedInVersionId;
        this.toVersionId = other.toVersionId;
    }

    public void updateLinksAtCreateInVersion(Version version) {
        createdInVersionId = version.getId();
        fromVersionId = version.getId();
        toVersionId = Long.MAX_VALUE;
        listId = version.getList().getId();
    }

    public void updateLinksAtCloneInVersion(Version version) {
        clonedInVersionId = version.getId();
        fromVersionId = version.getId();
        toVersionId = Long.MAX_VALUE;
    }

    public void updateLinksAtDeleteInVersion(Version version, Version previousVersion) {
        this.endAtVersion(previousVersion);
        deletedInVersionId = version.getId();
    }

    public void endAtVersion(Version version) { toVersionId = version.getId(); }

    public void updateLinksForPurge() {
        fromVersionId = Long.MAX_VALUE;
        toVersionId = -1L;
        listId = -1L;
    }

    public String printActiveInterval() { return "From version: " + fromVersionId + " - to version " + toVersionId; }
}
