
package com.bnpp.regliss.scheduler.cftgtw;

import com.bnpp.regliss.batch.service.HeartbeatService;
import com.bnpp.regliss.batch.service.NodeDetailsSupplier;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.CftgtwRequest;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.CftgtwRequestRepository;
import com.bnpp.regliss.repository.ReglissListRepository;
import com.bnpp.regliss.scheduler.CftSender;
import com.bnpp.regliss.vo.AutoFeedFileType;
import com.bnpp.regliss.vo.DJFeed;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ReglissPublisherProfile
@Slf4j
public class CftgtwRequestSender {

    private static final Logger cftgtwLog = LoggerFactory.getLogger("cftgtw");

    private static final String CFTGTW_FILENAME = "cftgtw.txt";
    private static final String WATCHLIST_XML_URL = "/xml/" ;
    private static final String WATCHLIST_EXTRA_XML_URL = "/xml/1158/"  ;
    private static final String CORE_XML_URL = "/sanctionalert/xml/1158/";
    private static final String AME_XML_URL = "/adversemedia/entities/xml/";
    private static final String WATCHLIST_CSV_URL = "/csv/" ;
    private static final String AME_CSV_URL = "/adversemedia/entities/csv/";
    private static final String FEEDLOGS_SUFFIX = "feedlogs/";

    @Value("${cft.gtw.hostname.WATCHLIST}")
    private String cftgtwHostnameWatchlist;
    @Value("${cft.gtw.hostname.WATCHLIST_EXTRA}")
    private String cftgtwHostnameWatchlistExtra;

    @Value("${cft.gtw.hostname.CORE}")
    private String cftgtwHostnameCore;

    @Value("${cft.gtw.hostname.AME}")
    private String cftgtwHostnameAme;

    @Value("${cft.gtw.idf.WATCHLIST}")
    private String cftgtwIdfWatchlist;

    @Value("${cft.gtw.idf.WATCHLIST_EXTRA}")
    private String cftgtwIdfWatchlistExtra;

    @Value("${cft.gtw.idf.CORE}")
    private String cftgtwIdfCore;

    @Value("${cft.gtw.idf.AME}")
    private String cftgtwIdfAme;

    @Value("${cft.gtw.part.WATCHLIST}")
    private String cftgtwPartWatchlist;

    @Value("${cft.gtw.part.WATCHLIST_EXTRA}")
    private String cftgtwPartWatchlistExtra;

    @Value("${cft.gtw.part.CORE}")
    private String cftgtwPartCore;

    @Value("${cft.gtw.part.AME}")
    private String cftgtwPartAme;

    @Value("${cft.gtw.path_to_file}")
    private String cftgtwPath;

    @Value("${cft.gtw.gmt.WATCHLIST}")
    String cftgtwGmtWatchlist;

    @Value("${cft.gtw.gmt.WATCHLIST_EXTRA}")
    String cftgtwGmtWatchlistExtra;

    @Value("${cft.gtw.gmt.CORE}")
    private String cftgtwGmtCore;

    @Value("${cft.gtw.gmt.AME}")
    private String cftgtwGmtAme;

    @Autowired
    private CftgtwRequestRepository cftgtwRequestRepo;

    @Autowired
    private ReglissListRepository listRepo;

    @Autowired
    private CftgtwFeedGenerator feedGenerator;

    @Autowired
    private CftSender cftSender;

    @Autowired
    private NodeDetailsSupplier nodeDetailsSupplier;

    @Autowired
    private HeartbeatService heartbeatService;

    @Scheduled(fixedDelayString = "${task.cftgtw_request_sender.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendScheduledCftgtwRequests() {
        if (heartbeatService.isNodeActivePublisher()) {
            sendCftGtwRequests();
        }
    }

    private void sendCftGtwRequests() {
        List<CftgtwRequest> requestsToSend = cftgtwRequestRepo.findByActualRunTimeIsNull();

        for (CftgtwRequest requestToSend : requestsToSend) {
            LocalDateTime cftGatewayTime = LocalDateTime.now(ZoneId.of(getCftgtwGmtByDJFormat(requestToSend.getImportFileType())));

            if(cftGatewayTime.isAfter(requestToSend.getRequestTime().minusSeconds(1))) {
                cftgtwLog.info("Sending new cftgtw requests for format: {} and time: {}", requestToSend.getImportFileType(),
                        requestToSend.getRequestTime());
                requestToSend.setActualRunTime(LocalDateTime.now());
                List<ReglissList> listsForFormat = listRepo.findByDJFormatNotDeleted(requestToSend.getImportFileType());
                List<String> cftLogTimeIds = new ArrayList<>();
                for (ReglissList reglissList : listsForFormat) {
                    try {
                        List<DJFeed> feedsToRequestForList = feedGenerator.generateFeedsForList(reglissList,
                                requestToSend.getRequestTime());
                        feedsToRequestForList.forEach(feed -> cftLogTimeIds.addAll(sendCftgtwFile(reglissList, feed)));
                    }catch (RuntimeException e){
                        //log error but continue for other lists/formats
                        log.error("Error trying to send CFTGW request for list {} - {}",reglissList.getReference(),reglissList.getShortName());
                        log.error("Error trying to send CFTGW request",e);
                    }
                }
                requestToSend.setRemainingCftLogTimeIds(cftLogTimeIds);
            }
        }
    }

    private List<String> sendCftgtwFile(ReglissList list, DJFeed feed) {
        ImportFileType listFormat = list.getImportConfiguration().getImportFileType();
        List<String> logTimeIds = new ArrayList<>();

        String cftgtwPart = getCftgtwPartByListFormat(listFormat);
        String cftgtwIdf = getCftgtwIdfByListFormat(listFormat);
        File cftgtwFile = new File(cftgtwPath, CFTGTW_FILENAME);

        Map<AutoFeedFileType, String> feedFiles = feed.getAllFeedFiles();
        for (Map.Entry<AutoFeedFileType, String> feedEntry : feedFiles.entrySet()) {
            String fileName = feedEntry.getValue();
            if (fileName != null) {
                try {
                    String logTimeId = cftSender.sendFile(cftgtwPart, cftgtwIdf, cftgtwFile.getAbsolutePath(),
                            getCftgtwParm(feedEntry.getKey(), listFormat, fileName));
                    cftgtwLog.info(
                            "Cftgtw request sent for list id: {}, with logTimeId: {}. Requested file with name: {} of type: {}",
                            list.getId(), logTimeId, fileName, feedEntry.getKey());
                    logTimeIds.add(logTimeId);
                } catch (RuntimeException e) {
                    cftgtwLog.error(
                            "Cftgtw request was not sent successfully for list id: {}. Requested file with name: {} of type: {}. Error message: {}",
                            list.getId(), fileName, feedEntry.getKey(), e.getMessage());
                }
            }
        }
        return logTimeIds;
    }

    private String getCftgtwIdfByListFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_CORE: return cftgtwIdfCore;
        case DOW_JONES_WATCHLIST: return cftgtwIdfWatchlist;
        case DOW_JONES_WATCHLIST_EXTRA: return cftgtwIdfWatchlistExtra;
        case DOW_JONES_AME: return cftgtwIdfAme;
        default: throw new IllegalArgumentException();
        }
    }

    private String getCftgtwPartByListFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_CORE: return cftgtwPartCore;
        case DOW_JONES_WATCHLIST: return cftgtwPartWatchlist;
        case DOW_JONES_WATCHLIST_EXTRA: return cftgtwPartWatchlistExtra;
        case DOW_JONES_AME: return cftgtwPartAme;
        default: throw new IllegalArgumentException();
        }
    }

    private String getCftgtwGmtByDJFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_CORE: return cftgtwGmtCore;
        case DOW_JONES_WATCHLIST: return cftgtwGmtWatchlist;
        case DOW_JONES_WATCHLIST_EXTRA: return cftgtwGmtWatchlistExtra;
        case DOW_JONES_AME: return cftgtwGmtAme;
        default: throw new IllegalArgumentException();
        }
    }

    private String getCftgtwParm(AutoFeedFileType feedType, ImportFileType listFormat, String fileName) {
        String parm = getCftgtwHostnameByListFormat(listFormat);
        if(feedType.isXml()){
            parm += getCftgtwXmlUrlByListFormat(listFormat);
        } else if(feedType.isCsv()){
            parm += getCftgtwCsvUrlByListFormat(listFormat);
        }
        if(feedType.isFeedLog()){
            parm += FEEDLOGS_SUFFIX;
        }
        parm += fileName;
        return parm;
    }

    private String getCftgtwHostnameByListFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_CORE: return cftgtwHostnameCore;
        case DOW_JONES_WATCHLIST: return cftgtwHostnameWatchlist;
        case DOW_JONES_WATCHLIST_EXTRA: return cftgtwHostnameWatchlistExtra;
        case DOW_JONES_AME: return cftgtwHostnameAme;
        default: throw new IllegalArgumentException();
        }
    }

    private String getCftgtwXmlUrlByListFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_CORE: return CORE_XML_URL;
        case DOW_JONES_WATCHLIST: return WATCHLIST_XML_URL;
        case DOW_JONES_WATCHLIST_EXTRA: return WATCHLIST_EXTRA_XML_URL;
        case DOW_JONES_AME: return AME_XML_URL;
        default: throw new IllegalArgumentException();
        }
    }

    private String getCftgtwCsvUrlByListFormat(ImportFileType listFormat) {
        switch(listFormat){
        case DOW_JONES_WATCHLIST: return WATCHLIST_CSV_URL;
        case DOW_JONES_AME: return AME_CSV_URL;
        default: throw new IllegalArgumentException();
        }
    }

}

