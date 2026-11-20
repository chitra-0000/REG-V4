package com.bnpp.regliss.scheduler;

import com.bnpp.regliss.batch.service.PurgeRecordsService;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ReglissBatchProfile
public class PurgeOrphanRecordsPoller {

    @Autowired
    private PurgeRecordsService purgeRecordsService;

    @Scheduled(cron = "${task.purgeOrphanRecords.cron}")
    public void purgeOrphanRecords() { purgeRecordsService.purgeOrphanRecordData(); }


}

