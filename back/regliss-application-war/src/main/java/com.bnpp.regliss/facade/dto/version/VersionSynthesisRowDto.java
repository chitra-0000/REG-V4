package com.bnpp.regliss.facade.dto.version;

import com.bnpp.regliss.annotation.AuditedAction;
import com.bnpp.regliss.annotation.ChildType;
import com.bnpp.regliss.entity.record.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VersionSynthesisRowDto {
    @JsonIgnore
    public long id;
    @JsonProperty("id")
    public String externalUuid;
    public String recordType;
    public String recordReglissReference;
    public String fullName;

    public VerificationStatus verificationStatus;
    public VerificationStatus finalVerificationStatus;
    public boolean isTemporaryVerificationStatus;
    public String verificationComment;
    public String finalVerificationComment;
    public boolean isTemporaryVerificationComment;
    public String modElement;
    public String newValue;
    public String oldValue;
    public Long recordId;

    public Long childId;
    public AuditedAction auditedAction;
    public ChildType childType;
}

