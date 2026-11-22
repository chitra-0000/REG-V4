package com.bnpp.regliss.vo;

import com.bnpp.regliss.entity.record.VerificationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class AddModSupLineForSynthesis {

    private final AddModSupLine addModSupLine;
    private final String externalUuid;
    private final String verificationComment;
    private final VerificationStatus verificationStatus;
    private final String userUid;
    private final LocalDateTime saveDate;
    private final String temporaryVerificationComment;
    private final VerificationStatus temporaryVerificationStatus;
    private final Long temporaryRecordId;


    public AddModSupLineForSynthesis(Object[] lineValues) {
        addModSupLine = new AddModSupLine(lineValues);
        externalUuid = (String) lineValues[14];
        verificationComment = (String) lineValues[15];
        verificationStatus =  lineValues[16] != null ? VerificationStatus.valueOf((String) lineValues[16]) : null;
        userUid =  (String) lineValues[17];
        saveDate = lineValues[18] != null ? ((Timestamp)lineValues[18]).toLocalDateTime(): null;
        temporaryVerificationComment =  (String) lineValues[19];
        temporaryVerificationStatus =  lineValues[20] != null ? VerificationStatus.valueOf((String) lineValues[20]) : null;
        temporaryRecordId = lineValues[21] != null ? ((BigDecimal)lineValues[21]).longValue() : null;
    }

}
