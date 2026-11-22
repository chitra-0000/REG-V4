package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.VersionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ReglissListSearchResultVersion {

    @JsonIgnore // Creates serialization infinite loop
    private final ReglissListSearchResult parentList;
    private final Long id;
    private final Long versionNumber;
    private final VersionStatus statusValue;
    private final LocalDateTime certificationDate;
    private final LocalDateTime publicationDate;
    private final LocalDate startDate;
    private boolean wasAutoUploaded;


    private boolean isLocked = false;
    private String lockingUser;

    private String status;
    private boolean isReadOnly;

    public ReglissListSearchResultVersion(ReglissListSearchResult parentList,
                                           Long id,
                                           Long versionNumber,
                                           VersionStatus statusValue,
                                           LocalDateTime certificationDate,
                                           LocalDateTime publicationDate,
                                           LocalDate startDate) {
        this.parentList = parentList;
        this.id = id;
        this.versionNumber = versionNumber;
        this.statusValue = statusValue;
        this.certificationDate = certificationDate;
        this.publicationDate = publicationDate;
        this.startDate = startDate;
    }


    public void setStatus(String status) { this.status = status; }

    public void setReadOnly(boolean readOnly) { isReadOnly = readOnly; }

    public void setLockedBy(String lockingUser) {
        isLocked = true;
        this.lockingUser = lockingUser;
    }


    public void setWasAutoUploaded(boolean wasAutoUploaded) { this.wasAutoUploaded = wasAutoUploaded; }
}
