package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.entity.batch.BatchExportType;
import com.bnpp.regliss.profile.ReglissWebProfile;
import com.bnpp.regliss.repository.FileDeliveryRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.service.FileGenerationDeliveryNotificationService;
import com.bnpp.regliss.service.ReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ReglissWebProfile
public class FileGenerationDeliveryNotificationPoller {

    //TODO move '%' to query
    private static final String ADD_MOD_SUP_EXPORT_TYPE = "%" + BatchExportType.ADD_MOD_SUP_FILE.toString() + "%";
    private static final String ADD_MOD_SUP_AUDIT_TRIAL_TYPE = "%" + BatchExportType.ADD_MOD_SUP_AUDIT_TRAIL_FILE.toString() + "%";

    @Autowired
    private FileDeliveryRepository fileDeliveryRepo;

    @Autowired
    private BatchJobExecutionRepository batchJobExecutionRepository;

    @Autowired
    private FileGenerationDeliveryNotificationService service;

    @Autowired
    private ReferenceService referenceService;

    private Long sysUserId;

    @PostConstruct
    public void init() {

        sysUserId = referenceService.getSysUser().getId();

        fileDeliveryRepo.getLatestStartedOrFinished()
                .ifPresent(service::saveLatestFileDeliveryDataInCache);

        batchJobExecutionRepository.getLastExportedJobOtherThanType(ADD_MOD_SUP_EXPORT_TYPE, ADD_MOD_SUP_AUDIT_TRIAL_TYPE, sysUserId)
                .ifPresent(service::saveLatestGenerationTimestampInCache);

        batchJobExecutionRepository.getLastExportedJobByType(ADD_MOD_SUP_EXPORT_TYPE, sysUserId)
                .ifPresent(service::saveLatestGenerationTimestampInCache);

        batchJobExecutionRepository.getLastExportedJobByType(ADD_MOD_SUP_AUDIT_TRIAL_TYPE, sysUserId)
                .ifPresent(service::saveLatestGenerationTimestampInCache);
    }

    @Scheduled(fixedDelayString = "${task.generate_delivery_file.interval}")
    public void notifyCompletionDeliveringFiles() {
        fileDeliveryRepo.getLatestStartedOrFinished()
                .ifPresent(service::notifyCompletionDeliveringFiles);

        batchJobExecutionRepository.getLastExportedJobOtherThanType(ADD_MOD_SUP_EXPORT_TYPE, ADD_MOD_SUP_AUDIT_TRIAL_TYPE, sysUserId)
                .ifPresent(service::notifyCompletionGeneratingFiles);

        batchJobExecutionRepository.getLastExportedJobByType(ADD_MOD_SUP_EXPORT_TYPE, sysUserId)
                .ifPresent(service::notifyCompletionGeneratingFiles);

        batchJobExecutionRepository.getLastExportedJobByType(ADD_MOD_SUP_AUDIT_TRIAL_TYPE, sysUserId)
                .ifPresent(service::notifyCompletionGeneratingFiles);
    }
}

