package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportMode;
import com.bnpp.regliss.entity.referential.ReferentialBusinessConcept;
import com.bnpp.regliss.entity.referential.record.RecordCategory;
import com.bnpp.regliss.entity.referential.record.RecordType;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.RecordCategoryRepository;
import com.bnpp.regliss.repository.WatchlistAmeRecordDeltaExportFilterRepository;
import com.bnpp.regliss.repository.WatchlistAmeRecordExportFilterRepository;
import com.bnpp.regliss.repository.WatchlistAmeRecordFullExportFilterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
// [MISSING FROM SCREENSHOTS: lines 26-29 of the original file were not photographed]
@ReglissBatchProfile
public class RecordsExportFilteringDataExtractor {

    @Autowired
    private WatchlistAmeRecordDeltaExportFilterRepository deltaFilterRepo;

    @Autowired
    private WatchlistAmeRecordFullExportFilterRepository fullFilterRepo;

    @Autowired
    private RecordCategoryRepository recordCategoryRepository;

    public List<RecordFilteringData> extractRecordFilteringData(Version version, FileExportMode exportMode) {
        Map<Long, RecordFilteringData> map = new HashMap<>();
        WatchlistAmeRecordExportFilterRepository filteringRepo = getRepoByExportMode(exportMode);

        log.info("Loading RCA category from referentials");
        int[] categoriesRCA = recordCategoryRepository.findByBusinessConcept(ReferentialBusinessConcept.RECORD_CATEGORY_RCA).stream()
                .mapToInt(rc->rc.getId().intValue()).toArray();

        log.info("Loading records-categories");
        List<Object[]> categoriesRawData = filteringRepo.getRecordCategories(version.getId());

        log.debug("Data back. Populating map...");
        for (Object[] arr : categoriesRawData) {
            map.put((Long) arr[0], new RecordFilteringData((Long) arr[0], (RecordType)arr[2], arr[3]!=null ?(Long) arr[3] :-1L, arr[4]!=null
                    ? (Long) arr[4] : -1L,categoriesRCA));
        }
        log.debug("Map created");
        extractLongs(RecordFilteringData::setCategories, categoriesRawData, map);

        log.info("Loading records-sanctionTypes");
        extractLongs(RecordFilteringData::setSanctionTypes, filteringRepo.getSanctionTypes(version.getId()), map);

        log.info("Loading records-description3");
        extractLongs(RecordFilteringData::setDescriptions3, filteringRepo.getRecordDescriptions3(version.getId()), map);

        log.info("Loading records-PEP occupations");
        extractLongs(RecordFilteringData::setPepOccupationCategories, filteringRepo.getRecordPepOccupations(version.getId()), map);

        log.info("Loading records to RESIDENCE countries");
        extractLongs(RecordFilteringData::setResidenceIds, filteringRepo.getRecordCountriesInVersionByBusinessConcept(version.getId(),
                ReferentialBusinessConcept.COUNTRY_TYPE_RESIDENCE), map);

        log.info("Loading records to JURISDICTION countries");
        extractLongs(RecordFilteringData::setJurisdictionIds, filteringRepo.getRecordCountriesInVersionByBusinessConcept(version.getId(),
                ReferentialBusinessConcept.COUNTRY_TYPE_JURISDICTION), map);

        log.info("Loading records to death dates");
        extractStrings(RecordFilteringData::setDeathDates,
                filteringRepo.getRecordDatesInVersionByBusinessConcept(version.getId(), ReferentialBusinessConcept.DATE_TYPE_DATE_OF_DEATH),map);
        log.info("Loading records to pep dates");
        extractCsvs(RecordFilteringData::setPepInactivityDates,
                filteringRepo.getRecordDatesInVersionByBusinessConcept(version.getId(), ReferentialBusinessConcept.DATE_TYPE_INACTIVE_AS_PEP),
                map);

        log.info("Loading records to rca dates");
        extractCsvs(RecordFilteringData::setRcaInactivityDates,
                filteringRepo.getRecordDatesInVersionByBusinessConcept(version.getId(), ReferentialBusinessConcept.DATE_TYPE_INACTIVE_AS_RCA),
                map);

        log.info("Loading records to gender types");
        extractLongs(RecordFilteringData::setGenderTypes,
                filteringRepo.getRecordGenders(version.getId()), map);
        return new ArrayList<>(map.values());
    }

    private void extractLongs(BiConsumer<RecordFilteringData, int[]> setter, List<Object[]> rawData, Map<Long, RecordFilteringData> map) {
        Map<Long, List<Long>> data = rawData.stream()
                .collect(
                    groupingBy(
                        recordIdMapper(),
                        mapping(arr -> (Long) arr[1], toList())
                    )
                );

        for (Map.Entry<Long, List<Long>> entry : data.entrySet()) {
            List<Long> list = entry.getValue();
            int[] array = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = Math.toIntExact(list.get(i));
            }
            setter.accept(map.get(entry.getKey()), array);
        }
    }

    private void extractStrings(BiConsumer<RecordFilteringData, String[]> setter, List<Object[]> rawData, Map<Long, RecordFilteringData> map) {
        Map<Long, List<String>> data = rawData.stream()
                .collect(
                    groupingBy(
                        recordIdMapper(),
                        mapping(arr -> (String) arr[1], toList())
                    )
                );
        for (Map.Entry<Long, List<String>> entry : data.entrySet()) {
            String[] array = entry.getValue().toArray(new String[0]);
            setter.accept(map.get(entry.getKey()), array);
        }
    }

    private void extractCsvs(BiConsumer<RecordFilteringData, String[]> setter, List<Object[]> rawData, Map<Long, RecordFilteringData> map) {
        // A map of Record ids and list of list of dates because it is possible that some dates are saved as comma separated values in db:
        // 22/03/2019,22/04/2020
        //TODO to verify if actually there are csv dates saved in db
        Map<Long, List<CommaSeparatedStringValues>> data = rawData.stream()
            .collect(
                groupingBy(
                    recordIdMapper(),
                    mapping(arr -> (new CommaSeparatedStringValues((String) arr[1])), toList())
                )
            );

        for (Map.Entry<Long, List<CommaSeparatedStringValues>> entry : data.entrySet()) {
            String[] array = entry.getValue().stream()
                .flatMap(CommaSeparatedStringValues::getValuesAsList)
                .toArray(String[]::new);
            setter.accept(map.get(entry.getKey()), array);
        }
    }

    private static class CommaSeparatedStringValues {

        private List<String> commaSeparatedValues;
        public CommaSeparatedStringValues(String commaSeparatedValues) {
            this.commaSeparatedValues = asList(commaSeparatedValues.split(","));
        }

        public Stream<String> getValuesAsList() { return new ArrayList<>(commaSeparatedValues).stream(); }
    }

    private Function<Object[], Long> recordIdMapper() { return (Object[] arr) -> (Long) arr[0]; }

    private WatchlistAmeRecordExportFilterRepository getRepoByExportMode(FileExportMode actualExportMode) {
        WatchlistAmeRecordExportFilterRepository filteringRepo;
        if (actualExportMode == FileExportMode.ADD_MOD) {
            filteringRepo = deltaFilterRepo;
        } else { // FULL
            filteringRepo = fullFilterRepo;
        }
        return filteringRepo;
    }

}
