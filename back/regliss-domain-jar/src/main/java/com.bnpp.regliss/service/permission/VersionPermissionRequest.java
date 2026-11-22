package com.bnpp.regliss.service.permission;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.VersionStatus;
import com.bnpp.regliss.entity.referential.Confidentiality;
import com.bnpp.regliss.entity.referential.Scope;
import com.bnpp.regliss.search.ReglissListSearchResultVersion;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

import static com.bnpp.regliss.entity.VersionStatus.CERTIFIED;

@EqualsAndHashCode(callSuper = true)
public class VersionPermissionRequest extends ListPermissionRequest {

    private final LocalDateTime certificationDate;
    private final LocalDateTime publicationDate;
    private final VersionStatus versionStatus;

    public VersionPermissionRequest(Long id,
                                    Confidentiality confidentiality,
                                    Scope scope,
                                    Long countryId,
                                    LocalDateTime certificationDate,
                                    LocalDateTime publicationDate,
                                    VersionStatus versionStatus) {
        super(id, confidentiality, scope, countryId);
        this.certificationDate = certificationDate;
        this.publicationDate = publicationDate;
        this.versionStatus = versionStatus;
    }

    public VersionPermissionRequest(LocalDateTime certificationDate,
                                    LocalDateTime publicationDate, VersionStatus versionStatus, ReglissList list) {
        super(list);
        this.certificationDate = certificationDate;
        this.publicationDate = publicationDate;
        this.versionStatus = versionStatus;
    }
    public VersionPermissionRequest(Version version, ReglissList list) {
        this(version.getCertificationDate(), version.getPublicationDate(), version.getStatus(), list);
    }

    public VersionPermissionRequest(Version version) {
        this(version, version.getList());
    }

    public VersionPermissionRequest(ReglissListSearchResultVersion versionColumnDto) {
        super(versionColumnDto.getParentList());
        publicationDate = versionColumnDto.getPublicationDate();
        certificationDate = versionColumnDto.getCertificationDate();
        versionStatus = versionColumnDto.getStatusValue();
    }

    public boolean wasNotCertified() {
        return !wasCertified();
    }

    public boolean wasCertified() {
        return certificationDate != null;
    }

    public boolean wasPublished() {
        return publicationDate != null;
    }

    public boolean wasNotPublished() {
        return !wasPublished();
    }

    public boolean hasConfirmedOrCertifiedStatus() {
        return versionStatus == CERTIFIED;
    }
}
