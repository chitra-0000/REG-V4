package com.bnpp.regliss.exporter.launcher.filtering.watchlistame;

import com.bnpp.regliss.entity.ImportFileType;
import com.bnpp.regliss.entity.DJExportFilter;
import com.bnpp.regliss.entity.DJExportSubfilter;
import com.bnpp.regliss.entity.DJSubfilterDateCriteria;
import com.bnpp.regliss.entity.referential.DJSubfilterOperator;
import com.bnpp.regliss.entity.referential.record.*;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.RecordCategoryRepository;
import com.bnpp.regliss.vo.FilteringReferentials;
import com.bnpp.regliss.vo.RecordDescriptions;
import com.bnpp.regliss.vo.RecordIdType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections.CollectionUtils.containsAny;

@Slf4j
@Service
@ReglissBatchProfile
public class RecordsExportFilteringService {
    @Autowired
    private RecordCategoryRepository recordCategoryRepo;

    public Map<RecordIdType, ExportedRelationTypes> filterRecordsInMemory(DJExportFilter filter,
                                                                          ExportWatchlistAmeFilteringData exportWatchlistAmeFilteringData) {
        Map<RecordIdType, ExportedRelationTypes> map = new HashMap<>();
        for (DJExportSubfilter subfilter : filter.getActiveSubfilters()) {
            Map<RecordIdType, ExportedRelationTypes> recordsMatchingSubfilter = getRecordsMatchingSubfilter(subfilter,
                    exportWatchlistAmeFilteringData);

            for (Map.Entry<RecordIdType, ExportedRelationTypes> entry : recordsMatchingSubfilter.entrySet()) {
                ExportedRelationTypes newRelationTypes = entry.getValue();
                if (map.containsKey(entry.getKey())) {
                    map.computeIfPresent(entry.getKey(), (k, oldRelationTypes) -> oldRelationTypes.merge(newRelationTypes));
                } else {
                    map.put(entry.getKey(), newRelationTypes);
                }
            }
        }
        return map;
    }

    private Map<RecordIdType, ExportedRelationTypes> getRecordsMatchingSubfilter(DJExportSubfilter subfilter,
                                                                                 ExportWatchlistAmeFilteringData filteringData) {
        log.info("Filtering records by subfilter id={}", subfilter.getId());

        log.debug("Filtering by Descriptions {}", subfilter.getResidenceCountries().size());
        Set<RecordIdType> filteredRecordIds = getRecordsFilteredByDescriptions(subfilter, filteringData);

        if (!subfilter.getResidenceCountries().isEmpty()) {
            log.debug("Filtering {} records by {} Residence Countries", filteredRecordIds.size(), subfilter.getResidenceCountries().size());
            filteredRecordIds = getRecordsFilteredByResidence(subfilter.getResidenceCountries(), filteringData, filteredRecordIds);
        }
        if (!subfilter.getJurisdictionCountries().isEmpty()) {
            log.debug("Filtering {} records by {} Jurisdiction Countries", filteredRecordIds.size(), subfilter.getJurisdictionCountries().size());
            filteredRecordIds = getRecordsFilteredByJurisdiction(subfilter.getJurisdictionCountries(), filteringData, filteredRecordIds);
        }
        if (subfilter.hasPepSipDeathDateCriteria()) {
            log.debug("Filtering {} PEP SIP records by Death Date", filteredRecordIds.size());
            filteredRecordIds = getRecordsFilteredByDeathPepSipDates(subfilter, filteringData.getRecordFilteringData(), filteredRecordIds);
        }
        if (subfilter.hasRCADeathDateCriteria()) {
            log.debug("Filtering {} RCA records by Death Date", filteredRecordIds.size());
            filteredRecordIds = getRecordsFilteredByDeathRcaDates(subfilter, filteringData.getRecordFilteringData(), filteredRecordIds);
        }
        if (subfilter.hasInactivityPepDateCriteria()) {
            log.debug("Filtering {} records by Pep Date", filteredRecordIds.size());
            filteredRecordIds = getRecordsFilteredByPepDates(subfilter, filteringData.getRecordFilteringData(), filteredRecordIds);
        }
        if (subfilter.hasInactivityRcaDateCriteria()) {
            log.debug("Filtering {} records by RCA Date", filteredRecordIds.size());
            filteredRecordIds = getRecordsFilteredByRcaDates(subfilter, filteringData.getRecordFilteringData(), filteredRecordIds);
        }
        if (!subfilter.getGenders().isEmpty()) {
            log.debug("Filtering {} records by {} Genders", filteredRecordIds.size(), subfilter.getGenders().size());
            filteredRecordIds = getRecordsFilteredByGenders(subfilter.getGenders(), filteringData, filteredRecordIds);
        }
        log.debug("Done Filtering records by subfilter id={}. Got {} matching record ids", subfilter.getId(), filteredRecordIds.size());

        ExportedRelationTypes subfilterRelationTypes = getSubfilterRelationTypes(subfilter);
        return filteredRecordIds.stream()
                .collect(toMap(k->k, k -> subfilterRelationTypes));
    }

    private ExportedRelationTypes getSubfilterRelationTypes(DJExportSubfilter subfilter) {
        if(!subfilter.isAll() && !subfilter.includeRca()) {
            return ExportedRelationTypes.NO_RELATION;
        }
        return new ExportedRelationTypes(subfilter.getRelationTypes().stream()
                .map(RelationType::getId)
                .collect(toList()));
    }

    public Set<RecordIdType> getRecordsFilteredByDescriptions(DJExportSubfilter subfilter, ExportWatchlistAmeFilteringData filteringData) {
        return filteringData.getRecordFilteringData().stream()
                .filter(data -> recordMatchesDescriptions(subfilter, getRecordDescriptions(data.getDescriptionIds(),
                        filteringData.getFilteringReferentials())))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }

    private RecordDescriptions getRecordDescriptions(RecordDescriptionIds recordDescriptions, FilteringReferentials filteringReferentials) {
        Set<RecordCategory> recordCategories = filteringReferentials.getCategories().stream()
                .filter(category -> recordDescriptions.containsCategory(category.getId()))
                .collect(toSet());
        Set<SanctionType> recordSanctionTypes = filteringReferentials.getSanctionTypes().stream()
                .filter(sanctionType -> recordDescriptions.containsSanctionType(sanctionType.getId()))
                .collect(toSet());
        Set<Description3> recordDescriptions3 = filteringReferentials.getDescriptions3().stream()
                .filter(description3 -> recordDescriptions.containsDescription3(description3.getId()))
                .collect(toSet());
        Set<PEPOccupationCategory> recordPepOccupationCategories = filteringReferentials.getPepOccupationCategories().stream()
                .filter(pepOccupation -> recordDescriptions.containsPepOccupationCategory(pepOccupation.getId()))
                .collect(toSet());
        return new RecordDescriptions(recordCategories, recordSanctionTypes, recordDescriptions3, recordPepOccupationCategories);
    }

    public Set<RecordIdType> getRecordsFilteredByResidence(Set<Country> subfilterCountries, ExportWatchlistAmeFilteringData filteringData,
                                                           Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by residence countries...");
        return filteringData.getRecordFilteringData().stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry -> recordMatchesCountries(subfilterCountries, entry.getResidenceIds(),
                        filteringData.getFilteringReferentials().getInternationalCountriesId()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }

    public Set<RecordIdType> getRecordsFilteredByJurisdiction(Set<Country> subfilterCountries, ExportWatchlistAmeFilteringData filteringData,
                                                              Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by jurisdiction countries...");
        return filteringData.getRecordFilteringData().stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry -> recordMatchesCountries(subfilterCountries, entry.getJurisdictionIds(),
                        filteringData.getFilteringReferentials().getInternationalCountriesId()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }
    public Set<RecordIdType> getRecordsFilteredByGenders(Set<Gender> subfilterGenders, ExportWatchlistAmeFilteringData filteringData,
                                                         Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by gender...");
        return filteringData.getRecordFilteringData().stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry -> recordMatchesGenders(subfilterGenders,entry.getGenderTypes()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }
    public Set<RecordIdType> getRecordsFilteredByDeathPepSipDates(DJExportSubfilter subfilter, List<RecordFilteringData> filteringData,
                                                                  Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by death dates PEP...");
        return filteringData.stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry->!entry.isOnlyRca())
                .filter(entry -> recordDatesMatchesCriteriaOrNoRecordDateOfThatType(subfilter.getDeathPepSipDateCriteria(),
                        entry.getDeathDates()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }
    public Set<RecordIdType> getRecordsFilteredByDeathRcaDates(DJExportSubfilter subfilter, List<RecordFilteringData> filteringData,
                                                               Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by death dates RCA...");
        return filteringData.stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry->entry.hasRca())
                .filter(entry -> recordDatesMatchesCriteriaOrNoRecordDateOfThatType(subfilter.getDeathRCADateCriteria(), entry.getDeathDates()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }

    public Set<RecordIdType> getRecordsFilteredByPepDates(DJExportSubfilter subfilter, List<RecordFilteringData> filteringData,
                                                          Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by PEP dates...");
        return filteringData.stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry -> recordDatesMatchesCriteriaOrNoRecordDateOfThatType(subfilter.getInactivityPepDateCriteria(),
                        entry.getPepInactivityDates()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }

    public Set<RecordIdType> getRecordsFilteredByRcaDates(DJExportSubfilter subfilter, List<RecordFilteringData> filteringData,
                                                          Set<RecordIdType> filteredRecordIds) {
        log.info("Filtering records by RCA dates...");
        return filteringData.stream()
                .filter(entry -> filteredRecordIds.contains(entry.getRecordIdType()))
                .filter(entry -> recordDatesMatchesCriteriaOrNoRecordDateOfThatType(subfilter.getInactivityRcaDateCriteria(),
                        entry.getRcaInactivityDates()))
                .map(RecordFilteringData::getRecordIdType)
                .collect(toSet());
    }

    private boolean recordMatchesCountries(Set<Country> subfilterCountries, int[] recordCountryIds, List<Long> internationalCountryIds) {
        Set<Long> subfilterCountryIds = subfilterCountries.stream().map(Country::getId).collect(toSet());
        if (subfilterCountryIds.isEmpty()) {
            return true;
        }
        List<Long> countryIdsList = IntStream.of(recordCountryIds).mapToLong(i->i).boxed().collect(toList());
        if(countryIdsList.stream().anyMatch(internationalCountryIds::contains)) {
            return true;
        }
        List<Long> commonCountryId=new ArrayList<>(countryIdsList);
        commonCountryId.retainAll(subfilterCountryIds);
        return CollectionUtils.isEmpty(commonCountryId) ? false : true;
    }

    private boolean recordMatchesGenders(Set<Gender> subfilterGenders, int[] gendersIds) {
        Set<Long> subfilteGenderIds = subfilterGenders.stream().map(Gender::getId).collect(toSet());
        if (subfilteGenderIds.isEmpty()) {
            return true;
        }
        List<Long> genderIdsList = IntStream.of(gendersIds).mapToLong(i->i).boxed().collect(toList());
        List<Long> commonCountryId=new ArrayList<>(genderIdsList);
        commonCountryId.retainAll(subfilteGenderIds);
        return CollectionUtils.isEmpty(commonCountryId) ? false : true;


    }

    public boolean recordMatchesDescriptions(DJExportSubfilter subfilter, RecordDescriptions recordDescriptions) {
        if (subfilter.isALL()) {
            return true;
        }

        Set<Description> recordDescLeaves = computeDescriptionLeaves(
                recordDescriptions.getCategories(),
                recordDescriptions.getSanctionTypes(),
                recordDescriptions.getDescriptions3());
        Set<Description> subfilterDescLeaves = computeDescriptionLeaves(
                Collections.singleton(subfilter.getDescription1().getRecordCategory()),
                subfilter.getSanctionTypes(),
                subfilter.getDescriptions3());
        for (Description recordDescription : recordDescLeaves) {
            if (subfilter.getFilter().getFormat() == ImportFileType.DOW_JONES_WATCHLIST &&
                    subfilterDoesNotContainPepOccCategoriesOfRecord(
                            subfilter.getPepOccupationCategories(),
                            recordDescriptions.getPepOccupationCategories(),
                            recordDescription)) {
                continue;
            }
            if (containsAny(subfilterDescLeaves, recordDescription.getMeAndAllAncestors())) {
                return true;
            }
        }
        return false;
    }

    private boolean recordDatesMatchesCriteriaOrNoRecordDateOfThatType(DJSubfilterDateCriteria criteria, String[] recordDates) {
        if (recordDates == null) {
            return false;
        }

        Optional<LocalDate> formattedRecordDate;
        if (DJSubfilterOperator.GREATER_THAN == criteria.getOperator() ||
                DJSubfilterOperator.GREATER_OR_EQUAL == criteria.getOperator()) {

            formattedRecordDate = Stream.of(recordDates)
                    .map(dateStr -> formatDate(dateStr, true))
                    .min(naturalOrder());

        } else {
            formattedRecordDate = Stream.of(recordDates)
                    .map(dateStr -> formatDate(dateStr, false))
                    .max(naturalOrder());
        }
        boolean doesNotHaveThatTypeOfDate = !formattedRecordDate.isPresent();
        boolean matchesCriteria = formattedRecordDate
                .filter(d -> recordDateMatchesFilter(criteria.getOperator(), criteria.getYears(), d))
                .isPresent();
        /*Per specification for date filters, if the record does not have that type of date export it:
         * death date filter -> all living persons and ones matching death date criterion
         * inactivity date filter. -> all active persons and ones matching inactivity date criterion
         * */
        return doesNotHaveThatTypeOfDate || matchesCriteria;
    }

    // WARNING: DO NOT TOUCH FROM PROD. ONLY FOR TESTING
    @Setter @Getter
    public Supplier<LocalDate> timeFactory = LocalDate::now;

    private boolean recordDateMatchesFilter(DJSubfilterOperator operator, Long years, LocalDate recordDate) {
        LocalDate comparationDate = timeFactory.get().minusYears(years);
        if (DJSubfilterOperator.GREATER_THAN == operator) {
            return recordDate.isBefore(comparationDate);
        }
        if (DJSubfilterOperator.GREATER_OR_EQUAL == operator) {
            return recordDate.isBefore(comparationDate) || recordDate.isEqual(comparationDate);
        }
        if (DJSubfilterOperator.LESS_THAN == operator) {
            return recordDate.isAfter(comparationDate);
        }
        if (DJSubfilterOperator.LESS_OR_EQUAL == operator) {
            return recordDate.isAfter(comparationDate) || recordDate.isEqual(comparationDate);
        }
        return true;
    }

    private LocalDate formatDate(String dateStr, boolean isGreater) {
        String[] splittedDate = dateStr.split("/");

        String day = getDay(splittedDate);
        String month = getMonth(splittedDate, isGreater);
        String year = splittedDate[splittedDate.length-1];

        String monthYearDateStr = "01-"+month+"-"+year;
        LocalDate formattedDate;
        try {
            formattedDate = LocalDate.parse(monthYearDateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy").withLocale(Locale.US));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Could not parse original date string '" + dateStr + "'", e);
        }

        if(isAValidDayInMonth(day, formattedDate)) {
            return formattedDate.withDayOfMonth(Integer.parseInt(day));
        } else {
            if (isGreater) {
                return formattedDate.withDayOfMonth(1);
            } else {
                return formattedDate.withDayOfMonth(formattedDate.lengthOfMonth());
            }
        }
    }

    private boolean isAValidDayInMonth(String day, LocalDate formattedDate) {
        return day.matches("([1-9]|0[1-9]|1[0-9]|2[0-9]|3[0-1])")
                && formattedDate.lengthOfMonth() >= Integer.parseInt(day);
    }

    private String getDay(String[] splittedDate) {
        if(splittedDate.length == 3) {
            return splittedDate[0];
        }
        return "";
    }

    private String getMonth(String[] splittedDate, boolean isGreater) {
        String monthStr = "";
        if(splittedDate.length > 1) {
            monthStr = splittedDate[splittedDate.length - 2];
        }

        if(isAValidMonth(monthStr)) {
            return monthStr;
        } else {
            if(isGreater) {
                return DateTimeFormatter.ofPattern("MM").format(Month.JANUARY);
            }else {
                return DateTimeFormatter.ofPattern("MM").format(Month.DECEMBER);
            }
        }
    }

    private boolean isAValidMonth(String monthStr) {
        try {
            Month month = Month.from(DateTimeFormatter.ofPattern("MM").parse(monthStr));
            return month != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean subfilterDoesNotContainPepOccCategoriesOfRecord(Set<PEPOccupationCategory> subfilterPepOccupationCategories,
                                                                    Set<PEPOccupationCategory> recordPepOccupationCategories,
                                                                    Description recordDescription) {
        return recordDescription.isOnPEPSubtree() && !subfilterPepOccupationCategories.isEmpty()
                && !containsAny(subfilterPepOccupationCategories, recordPepOccupationCategories);
    }

    private static Set<Description> computeDescriptionLeaves(
            Set<RecordCategory> categoriesParam,
            Set<SanctionType> sanctionTypesParam,
            Set<Description3> descriptions3) {

        Set<RecordCategory> categories = new HashSet<>(categoriesParam);
        Set<SanctionType> sanctionTypes = new HashSet<>(sanctionTypesParam);
        Set<Description> leaves = new HashSet<>();

        for (Description3 description3 : descriptions3) {
            leaves.add(description3);
            sanctionTypes.remove(description3.getSanctionType());
            categories.remove(description3.getSanctionType().getRecordCategory());
        }
        for (SanctionType sanctionType : sanctionTypes) {
            leaves.add(sanctionType);
            categories.remove(sanctionType.getRecordCategory());
        }

        leaves.addAll(categories);
        return leaves;
    }
}
