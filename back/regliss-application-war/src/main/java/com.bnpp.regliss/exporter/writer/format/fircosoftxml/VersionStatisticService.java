package com.bnpp.regliss.exporter.writer.format.fircosoftxml;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.VersionDJ;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ReglissBatchProfile
public class VersionStatisticService {
    @Autowired
    private VersionService versionService;

    @Transactional
    public List<ListPVLivraison> getAllOtherLastVersionStatistics(Long excludedListId) {
        return versionService.getAllOtherLatestPublishedVersions(excludedListId).stream().map(VersionStatisticService::toVersionPVList)
                .collect(Collectors.toList());
    }



    @Transactional
    public ListPVLivraison getSanctionVersionStatistics(Version version) { return toVersionPVList(version); }

    private static ListPVLivraison toVersionPVList(Version version) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter startDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ListPVLivraison listStat = new ListPVLivraison();
        ReglissList list = version.getList();
        listStat.versionId=version.getId();
        listStat.listName = list.getShortName();
        listStat.refVigilance = list.getReference().replaceFirst("^0+", "");
        listStat.version = version.getVersionNumber().toString();
        listStat.versionOffDate = startDateFormatter.format(version.getStartDate())+" 00:00:00";
        listStat.versionPublishDate = version.getPublicationDate()!=null?formatter.format(version.getPublicationDate()):"";
        VersionRecStatsPV logStats = new VersionRecStatsPV();
        VersionDJ versionDJ = version.getVersionDJ();
        if (versionDJ == null || versionDJ.getLogRecordNumbers()==null || versionDJ.getLogRecordNumbers().getTotal() == null) {
            if (version.getComputedRecordNumbers() != null) {
                logStats.recordTotalCount = version.getComputedRecordNumbers().getTotal();
                logStats.recordAddCount = version.getComputedRecordNumbers().getAdd();
                logStats.recordModCount = version.getComputedRecordNumbers().getMod();
                logStats.recordSuppCount = version.getComputedRecordNumbers().getSup();
            }
        } else {
            logStats.recordTotalCount = versionDJ.getLogRecordNumbers().getTotal();
            logStats.recordAddCount = versionDJ.getLogRecordNumbers().getAdd();
            logStats.recordModCount = versionDJ.getLogRecordNumbers().getMod();
            logStats.recordSuppCount = versionDJ.getLogRecordNumbers().getSup();
        }
        listStat.versionRecStatsPV = logStats;
        VersionAliasStatsPV recStats = new VersionAliasStatsPV();
        if (version.getComputedRecordNumbers() != null) {
            recStats.aliasAddCount = version.getComputedRecordNumbers().getAdd();
            recStats.aliasModCount = version.getComputedRecordNumbers().getMod();
            recStats.aliasSuppCount = version.getComputedRecordNumbers().getSup();
            recStats.aliasTotalCount = version.getComputedRecordNumbers().getWeak()
                    + version.getComputedRecordNumbers().getStrong();
            recStats.aliasWeakCount = version.getComputedRecordNumbers().getWeak();
            recStats.aliasStrongCount = version.getComputedRecordNumbers().getStrong();
        }
        listStat.versionAliasStatsPV = recStats;
        return listStat;
    }
}

