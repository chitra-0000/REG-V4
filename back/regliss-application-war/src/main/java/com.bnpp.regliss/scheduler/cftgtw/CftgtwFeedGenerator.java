package com.bnpp.regliss.scheduler.cftgtw;

import com.bnpp.regliss.batch.service.ImportConfigurationSupplier;
import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.profile.ReglissPublisherProfile;
import com.bnpp.regliss.repository.ImportedFileRepository;
import com.bnpp.regliss.vo.AutoFeedFileType;
import com.bnpp.regliss.vo.DJFeed;
import com.bnpp.regliss.vo.DJFeedId;
import com.bnpp.regliss.vo.DJFileImportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.bnpp.regliss.utils.DJFilenameHelper.*;

@ReglissPublisherProfile
@Service
public class CftgtwFeedGenerator {
    @Autowired
    private ImportConfigurationSupplier importConfigurationSupplier;

    @Autowired
    private ImportedFileRepository importedFileRepository;

    public List<DJFeed> generateFeedsForList(ReglissList list, LocalDateTime requestTime) {
        List<DJFeed> feeds = new ArrayList<>();
        ImportFileType listFormat = list.getImportConfiguration().getImportFileType();
        List<DJFileImportMode> djFileImportModes = getDJImportModesFor(list);
        if(!importConfigurationSupplier.isIncrementalAllowedForDJFormat(listFormat) || !list.getImportConfiguration().allowIncrementalImport()){
            djFileImportModes=djFileImportModes.stream().filter(fim-> fim != DJFileImportMode.INCREMENTAL).collect(Collectors.toList());
        }
        for (DJFileImportMode importMode : djFileImportModes) {
            DJFeedId feedId = new DJFeedId(list.getImportConfiguration().getFileNamePrefix(),
                    getDateStamp(requestTime, listFormat), importMode);
            DJFeed feed = new DJFeed(feedId);
            for (AutoFeedFileType feedFileType : feed.getRequiredFileTypes(list.getImportConfiguration())) {
                feed.setFileNameForType(feedFileType, computeFileName(feedFileType, listFormat, feedId));
            }
            feeds.add(feed);
        }
        return feeds;
    }

    private List<DJFileImportMode> getDJImportModesFor(ReglissList list) {
        if (list.getImportConfiguration().isWatchlist()||list.getImportConfiguration().isWatchlistExtra()){
            if(fullImportWasAlreadyDoneCurrentMonth(list)){
                return Collections.singletonList(DJFileImportMode.INCREMENTAL);
            }else{
                return Collections.singletonList(DJFileImportMode.FULL);
            }
        }
        else return list.getDJFileImportModes();
    }

    private boolean fullImportWasAlreadyDoneCurrentMonth(ReglissList list) {
        return importedFileRepository.countFullImportsInMonth(list.getId())>0;
    }

    private String computeFileName(AutoFeedFileType feedType, ImportFileType listFormat, DJFeedId feedId) {
        switch (feedType) {
            case XML_DATA_FILE:
                return feedId.getPrefix() + "_" + feedId.getDateStamp() + "_" + getImportModeSuffix(feedId) + ".zip";
            case XML_CHECKSUM_FILE:
                return CHECKSUM_PREFIX + "_" + feedId.getPrefix() + "_" + feedId.getDateStamp() + "_" + getImportModeSuffix(feedId) + ".txt";
            case XML_LOG_FILE:
                return computeXmlLogFileName(feedId);
            case CSV_DATA_FILE:
                return getWlAmeCsvPrefix(listFormat) + "_" + feedId.getDateStamp() + "_" + getImportModeSuffix(feedId) + ".zip";
            case CSV_CHECKSUM_FILE:
                return CHECKSUM_PREFIX + "_" + getWlAmeCsvPrefix(listFormat) + "_" + feedId.getDateStamp() + "_" + getImportModeSuffix(feedId) + ".txt";
            case CSV_LOG_FILE:
                return computeCsvLogFileName(feedId, listFormat);
            default:
                throw new IllegalArgumentException();
        }
    }

    private String computeXmlLogFileName(DJFeedId feedId) {
        if (feedId.getImportMode() == DJFileImportMode.FULL) {
            return feedId.getPrefix() + "_" + feedId.getDateStamp() + ".txt";
        } else {
            return null;
        }
    }

    private String computeCsvLogFileName(DJFeedId feedId, ImportFileType listFormat) {
        if (feedId.getImportMode() == DJFileImportMode.FULL) {
            return getWlAmeCsvPrefix(listFormat) + "_" + feedId.getDateStamp() + ".txt";
        } else {
            return null;
        }
    }

    private String getImportModeSuffix(DJFeedId feedId) {
        return feedId.getImportMode().toImportFileNameSuffix().toLowerCase();
    }

    private static String getDateStamp(LocalDateTime requestTime, ImportFileType importFileType) {
        return requestTime.format(DJ_DATE_FORMATTER) + getTimeSuffix(requestTime, importFileType);
    }

    private static String getTimeSuffix(LocalDateTime requestTime, ImportFileType importFileType) {
        switch (importFileType) {
            case DOW_JONES_CORE:
                return getTimeStampForCore(requestTime);
            case DOW_JONES_WATCHLIST:
                return WATCHLIST_TIME_SUFFIX;
            case DOW_JONES_AME:
                return AME_TIME_SUFFIX;
            case DOW_JONES_WATCHLIST_EXTRA:
                return WATCHLIST_EXTRA_TIME_SUFFIX;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getTimeStampForCore(LocalDateTime requestTime) {
        LocalTime time = requestTime.toLocalTime();
        if (time.isBefore(LocalTime.of(8, 0))) {
            return "_1";
        } else if (time.isBefore(LocalTime.of(16, 0))) {
            return "_2";
        } else {
            return "_3";
        }
    }

}

