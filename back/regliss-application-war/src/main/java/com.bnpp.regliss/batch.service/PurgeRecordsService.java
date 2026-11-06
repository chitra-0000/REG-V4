package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.repository.RecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class PurgeRecordsService {

    private static final String PURGE_ORPHAN_RECORD_SUCCES = "SUCCES";

    @Autowired
    private RecordRepository recordRepo;
    @Autowired
    private BatchManagementService batchManagementService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgeOrphanRecordData(){
        if(batchManagementService.currentNodeIsAllowedToStartImportAuto()) {
            log.info("Call PURGE_ORPHAN_RECORD_DATA");
            String returnMsg = recordRepo.purgeOrphanRecordData();
            if (!PURGE_ORPHAN_RECORD_SUCCES.equals(returnMsg)) {
                throw new ReglissException("PURGE_ORPHAN_RECORD_DATA call failed with error message: " + returnMsg);
            }
            log.debug("Finish PURGE_ORPHAN_RECORD_DATA");
        }
    }
}
