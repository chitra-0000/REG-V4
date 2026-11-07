package com.bnpp.regliss.exporter.writer.format.dj.watchlistxml;

import com.bnpp.regliss.entity.*;
import com.bnpp.regliss.exporter.launcher.filtering.watchlistame.CounterKeys;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.service.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Map;


@Slf4j
@Service
@ReglissBatchProfile
public class VersionWatchlistStatisticService {
    @Autowired
    private VersionService versionService;


    @Transactional
    public ListWatchlistPVLivraison getWatchlistVersionStatistics(Version version, Map<CounterKeys,Long> statistics) {
        return toVersionPVWatclitsList(version,statistics);
    }

    private static ListWatchlistPVLivraison toVersionPVWatclitsList(Version version, Map<CounterKeys,Long> statistics) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter startDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ListWatchlistPVLivraison listStat = new ListWatchlistPVLivraison();
        ReglissList list = version.getList();
        listStat.versionId=version.getId();
        listStat.listName = list.getShortName();
        listStat.version = version.getVersionNumber().toString();
        listStat.versionOffDate = startDateFormatter.format(version.getStartDate())+" 00:00:00";
        listStat.versionPublishDate = version.getPublicationDate()!=null?formatter.format(version.getPublicationDate()):"";
        RecordsPVLivraison recordsDb = new RecordsPVLivraison();
        recordsDb.comment = ListWatchlistComments.DATABASE_RECORDS.getComment();
        AddModSupPv add=new AddModSupPv();
        AddModSupPv mod=new AddModSupPv();
        AddModSupPv sup=new AddModSupPv();
        ComputedRecordNumbers computedRecordNumbers=version.getComputedRecordNumbers();
        if (computedRecordNumbers != null) {
            recordsDb.count = computedRecordNumbers.getTotal();
            mod.count=computedRecordNumbers.getMod();
            add.count=computedRecordNumbers.getAdd();
            sup.count=computedRecordNumbers.getSup();
            mod.persons=computedRecordNumbers.getModPersons()==null?0:computedRecordNumbers.getModPersons();
            mod.entities=mod.count-mod.persons;
            add.persons=computedRecordNumbers.getAddPersons()==null?0:computedRecordNumbers.getAddPersons();
            add.entities=add.count-add.persons;
            sup.persons=computedRecordNumbers.getSupPersons()==null?0:computedRecordNumbers.getSupPersons();
            sup.entities=sup.count-sup.persons;
        }
        recordsDb.mod=mod;
        recordsDb.sup=sup;
        recordsDb.add=add;
        add=new AddModSupPv();
        mod=new AddModSupPv();
        sup=new AddModSupPv();
        RecordsPVLivraison recordsLog = new RecordsPVLivraison();
        recordsLog.comment = ListWatchlistComments.DJ_LOG_RECORDS.getComment();
        VersionDJ versionDJ = version.getVersionDJ();
        if(versionDJ!=null) {
            listStat.name = version.getFileName();
            LogRecordNumbers logRecordNumbers = versionDJ.getLogRecordNumbers();
            recordsLog.count = 0L;
            if (logRecordNumbers != null) {

                recordsLog.count = logRecordNumbers.getTotal();
                add.count = logRecordNumbers.getAdd();
                add.entities = logRecordNumbers.getAddedEntities();
                add.persons = logRecordNumbers.getAddedPersons();
                mod.count = logRecordNumbers.getMod();
                mod.entities = logRecordNumbers.getModEntities();
                mod.persons = logRecordNumbers.getModPersons();
                sup.count = logRecordNumbers.getSup();
                sup.persons = logRecordNumbers.getSubPersons();
                sup.entities = logRecordNumbers.getSubEntities();


            }
        }
        recordsLog.mod=mod;
        recordsLog.sup=sup;
        recordsLog.add=add;
        RecordsPVLivraison filteredRecords = new RecordsPVLivraison();
        filteredRecords.comment = ListWatchlistComments.FILTERED_RECORDS.getComment();
        add=new AddModSupPv();
        mod=new AddModSupPv();
        filteredRecords.included=statistics.get(CounterKeys.TOTAL);
        add.count = statistics.get(CounterKeys.TOTALADD);
        add.persons = statistics.get(CounterKeys.PERSONADD);
        add.entities = add.count-add.persons;
        mod.count =  statistics.get(CounterKeys.TOTALMOD);
        mod.persons = statistics.get(CounterKeys.PERSONMOD);
        mod.entities = mod.count-mod.persons;
        filteredRecords.add=add;
        filteredRecords.mod=mod;

        AddModSupPv excluded=new AddModSupPv();
        excluded.comment = ListWatchlistComments.EXCLUDED_RECORDS.getComment();
        excluded.count=recordsDb.count-filteredRecords.included;
        excluded.persons=recordsDb.add.persons+ recordsDb.mod.persons-filteredRecords.add.persons-filteredRecords.mod.persons;
        excluded.entities=recordsDb.add.entities+ recordsDb.mod.entities-filteredRecords.add.entities-filteredRecords.mod.entities;
        filteredRecords.excluded=excluded;
        listStat.records=recordsLog;
        listStat.databaseRecords=recordsDb;
        listStat.filteredRecords=filteredRecords;
        return listStat;
    }
}
