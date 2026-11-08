package com.bnpp.regliss.facade.dto.version;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class VersionSynthesisRecordDto {
    @JsonIgnore
    public long id;
    @JsonProperty("id")
    public String externalUuid;
    public String recordType;
    public String recordReglissReference;
    public String fullNameRaw;
    public String verificationStatus;
    public String comments;
    public List<SynthesysAuditedFieldDto> auditedFields;
}

