package com.bnpp.regliss.facade.dto.record;


// [MISSING FROM SCREENSHOTS: lines 4-8 of the original file were not visible in the photos]

public class RecordVerificationInfoDto {
    public String verificationComment;
    public VerificationStatus verificationStatus;
    public Long recordId;
    public Long childId;
    public AuditedAction auditedAction;
    public ChildType childType;
    public Long userId;
    public LocalDateTime saveDate;
}

