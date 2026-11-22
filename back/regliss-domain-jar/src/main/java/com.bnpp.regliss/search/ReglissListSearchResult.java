package com.bnpp.regliss.search;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ReglissListType;
import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class ReglissListSearchResult  {
    private final long id;
    private final boolean active;
    private final ReglissListType type;
    private final Long listCategory;
    private final Scope scope;
    private final Long country;
    private final String name;
    private final Confidentiality confidentiality;
    private final String reference;
    private final boolean allowAutoUpload;
    private final boolean allowManualUpload;
    private final ImportFileType importFileType;
    private final ReglissListSearchResultVersion applicableVersion;
    private final ReglissListSearchResultVersion currentVersion;
    private boolean isReadOnly;
    private boolean isVersionReadOnly;

    public ReglissListSearchResult(
            long id,
            boolean active,
            ReglissListType type,
            Long listCategory,
            Scope scope,
            Long country,
            String name,
            Confidentiality confidentiality,
            String reference,
            boolean allowAutoUpload,
            boolean allowManualUpload,
            ImportFileType importFileType,

            Long appVerId,
            Long appVerNumber,
            VersionStatus appVerStatusValue,
            LocalDateTime appVerCertificationDate,
            LocalDateTime appVerPublicationDate,
            LocalDate appVerStartDate,
            boolean appVerWasAutoUploaded,

            Long curVerId,
            Long curVerNumber,
            VersionStatus curVerStatusValue,
            LocalDateTime curVerCertificationDate,
            LocalDateTime curVerPublicationDate,
            LocalDate curVerStartDate,
            boolean curVerWasAutoUploaded) {
        this.id = id;
        this.active = active;
        this.type = type;
        this.listCategory = listCategory;
        this.scope = scope;
        this.country = country;
        this.name = name;
        this.confidentiality = confidentiality;
        this.reference = reference;
        this.allowAutoUpload = allowAutoUpload;
        this.allowManualUpload = allowManualUpload;
        this.importFileType = importFileType;

        if (appVerId != null) {
            applicableVersion = new ReglissListSearchResultVersion(
                    this,
                    appVerId,
                    appVerNumber,
                    appVerStatusValue,
                    appVerCertificationDate,
                    appVerPublicationDate,
                    appVerStartDate);
            applicableVersion.setWasAutoUploaded(appVerWasAutoUploaded);
        } else {
            applicableVersion = null;
        }
        if (curVerId != null) {
            currentVersion = new ReglissListSearchResultVersion(
                    this,
                    curVerId,
                    curVerNumber,
                    curVerStatusValue,
                    curVerCertificationDate,
                    curVerPublicationDate,
                    curVerStartDate);
            currentVersion.setWasAutoUploaded(curVerWasAutoUploaded);
        } else {
            currentVersion = null;
        }
    }


    public void setReadOnly(boolean readOnly) { isReadOnly = readOnly; }

    public void setVersionReadOnly(boolean versionReadOnly) { isVersionReadOnly = versionReadOnly; }

    public Stream<ReglissListSearchResultVersion> getAllVersionsDisplayedStream() {
        List<ReglissListSearchResultVersion> versions = new ArrayList<>();
        if (applicableVersion != null) {
            versions.add(applicableVersion);
        }
        if (currentVersion != null) {
            versions.add(currentVersion);
        }
        return versions.stream();
    }
}
