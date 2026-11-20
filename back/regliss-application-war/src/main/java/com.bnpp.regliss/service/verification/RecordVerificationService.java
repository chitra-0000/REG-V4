package com.bnpp.regliss.service.verification;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.app.RecordVerificationChanges;
import com.bnpp.regliss.entity.app.RecordVerificationInfo;
import com.bnpp.regliss.facade.dto.record.RecordVerificationInfoDto;
import com.bnpp.regliss.facade.mapper.RecordVerificationChangesMapper;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.RecordVerificationChangesRepository;
import com.bnpp.regliss.repository.RecordVerificationInfoRepository;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.service.VersionAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ReglissWebProfile
public class RecordVerificationService {

    @Autowired
    private RecordVerificationInfoRepository recordVerificationInfoRepository;

    @Autowired
    private RecordVerificationChangesRepository recordVerificationChangesRepository;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private VersionAuditService versionAuditService;

    @Autowired
    private RecordVerificationChangesMapper recordVerificationChangesMapper;

    public void insertOrUpdateRecordVerificationChanges(RecordVerificationInfoDto verifiedRecordDto, Version version) {
        RecordVerificationChanges oldRecordVerificationChanges = recordVerificationChangesRepository.findByVersionIdAndRecordIdAndChildIdAndChildTypeAndAuditedAction(
                version.getId(),
                verifiedRecordDto.recordId,
                verifiedRecordDto.childId,
                verifiedRecordDto.childType,
                verifiedRecordDto.auditedAction
        ).orElse(RecordVerificationChanges.emptyWithIds(
                version.getId(),
                verifiedRecordDto.recordId,
                verifiedRecordDto.childId,
                verifiedRecordDto.childType,
                verifiedRecordDto.auditedAction
        ));

        RecordVerificationChanges newRecordVerificationChanges = new RecordVerificationChanges(oldRecordVerificationChanges);
        newRecordVerificationChanges.setVerificationComment(verifiedRecordDto.verificationComment);
        newRecordVerificationChanges.setVerificationStatus(verifiedRecordDto.verificationStatus);
        newRecordVerificationChanges.setUserId(requestContext.getCurrentUser().getId());
        newRecordVerificationChanges.setSaveDate(LocalDateTime.now());
        recordVerificationChangesRepository.save(newRecordVerificationChanges);
    }

    public void updateVerificationOnSaveOrVerify(Version version){
        List<RecordVerificationChanges> listTemporaryRecords = recordVerificationChangesRepository.findByVersionId(version.getId());
        listTemporaryRecords.forEach(recordVerificationChanges ->  updateRecordVerificationInfoAndChanges(recordVerificationChanges, version));
    }

    public void deleteTemporaryVerificationCommentsStatusVersion(Long versionId){
        recordVerificationChangesRepository.findByVersionId(versionId)
                .forEach(recordVerificationChanges -> recordVerificationChangesRepository.delete(recordVerificationChanges));
    }

    public void updateVersionStatusAndComment(RecordVerificationInfoDto dto, Version versionFromDb) {
        insertOrUpdateRecordVerificationInfo(dto,versionFromDb);
    }

    private void updateRecordVerificationInfoAndChanges(RecordVerificationChanges recordVerificationChanges, Version version){
        recordVerificationChangesRepository.delete(recordVerificationChanges);
        insertOrUpdateRecordVerificationInfo(recordVerificationChangesMapper.toRecordVerificationInfoDto(recordVerificationChanges), version);
    }

    private void insertOrUpdateRecordVerificationInfo(RecordVerificationInfoDto verifiedRecordDto, Version version) {
        RecordVerificationInfo oldRecordVerificationInfo = recordVerificationInfoRepository.findByVersionIdAndRecordIdAndChildIdAndChildTypeAndAuditedAction(
                version.getId(),
                verifiedRecordDto.recordId,
                verifiedRecordDto.childId,
                verifiedRecordDto.childType,
                verifiedRecordDto.auditedAction
        ).orElse(RecordVerificationInfo.emptyWithIds(
                version.getId(),
                verifiedRecordDto.recordId,
                verifiedRecordDto.childId,
                verifiedRecordDto.childType,
                verifiedRecordDto.auditedAction
        ));

        RecordVerificationInfo newRecordVerificationInfo = new RecordVerificationInfo(oldRecordVerificationInfo);
        newRecordVerificationInfo.setVerificationComment(verifiedRecordDto.verificationComment);
        newRecordVerificationInfo.setVerificationStatus(verifiedRecordDto.verificationStatus);
        newRecordVerificationInfo.setUserId(requestContext.getCurrentUser().getId());
        newRecordVerificationInfo.setSaveDate(verifiedRecordDto.saveDate);
        versionAuditService.auditRecordVerificationInfo(oldRecordVerificationInfo, newRecordVerificationInfo, version);
        recordVerificationInfoRepository.save(newRecordVerificationInfo);
    }
}

