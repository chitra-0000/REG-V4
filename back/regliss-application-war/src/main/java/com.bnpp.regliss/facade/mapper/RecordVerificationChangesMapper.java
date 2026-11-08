package com.bnpp.regliss.facade.mapper;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

@Service
@ReglissWebProfile
public class RecordVerificationChangesMapper {

    public RecordVerificationInfoDto toRecordVerificationInfoDto(RecordVerificationChanges recordVerificationChanges){
            RecordVerificationInfoDto recordVerificationInfoDto = new RecordVerificationInfoDto();
            recordVerificationInfoDto.recordId = recordVerificationChanges.getRecordId();
            recordVerificationInfoDto.childId = recordVerificationChanges.getChildId();
            recordVerificationInfoDto.auditedAction = recordVerificationChanges.getAuditedAction();
            recordVerificationInfoDto.childType = recordVerificationChanges.getChildType();
            recordVerificationInfoDto.verificationComment = recordVerificationChanges.getVerificationComment();
            recordVerificationInfoDto.verificationStatus = recordVerificationChanges.getVerificationStatus();
            recordVerificationInfoDto.userId = recordVerificationChanges.getUserId();
            recordVerificationInfoDto.saveDate = recordVerificationChanges.getSaveDate();
            return recordVerificationInfoDto;
    }
}
