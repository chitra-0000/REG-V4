package com.bnpp.regliss.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.repository.VersionRepository;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RefreshRecordSearchViewsLauncher {

    @Autowired
    private VersionRepository versionRepo;

    private static final String REFRESH_MV_VIEW_REGLISS_SUCCES = "SUCCES";
    private static final int REFRESH_HISTORICAL_VIEW = 1;
    private static final int REFRESH_CURRENT_VIEW = 0;
    public void refreshRecordSearchCurrentVersionView(){
        log.info("Call REFRESH_MV_REGLISS for current version");
        String returnMsg = versionRepo.refreshSearchRecordView(REFRESH_CURRENT_VIEW);
        if (!REFRESH_MV_VIEW_REGLISS_SUCCES.equals(returnMsg)){
            throw new ReglissException("REFRESH_MV_REGLISS call failed with error message: "+returnMsg);
        }
        log.debug("Finish REFRESH_MV_REGLISS for current version");
    }

    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void asyncRefeshRecordSearchHistoricalVersionsView() {
        log.info("Call REFRESH_MV_REGLISS for historical versions");
        String returnMsg = versionRepo.refreshSearchRecordView(REFRESH_HISTORICAL_VIEW);
        if (!REFRESH_MV_VIEW_REGLISS_SUCCES.equals(returnMsg)){
            throw new ReglissException("REFRESH_MV_REGLISS call failed with error message: "+returnMsg);
        }
        log.debug("Finish REFRESH_MV_REGLISS for historical versions");
    }
// TODO: [GAP - end of class/file not captured in screenshots after line 41]
