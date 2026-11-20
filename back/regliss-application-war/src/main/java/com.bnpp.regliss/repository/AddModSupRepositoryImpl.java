package com.bnpp.regliss.repository;

import com.bnpp.regliss.entity.record.Record;
import com.bnpp.regliss.repository.pagination.PaginationOptions;
import com.bnpp.regliss.utils.PerformanceUtil;
import com.bnpp.regliss.vo.*;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.bnpp.regliss.vo.AddModSupCountType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

@Slf4j
/unchecked/
@Repository
public class AddModSupRepositoryImpl extends BaseRepositoryImpl<Record> implements AddModSupRepositoryCustom {

    private static final List<AddModSupCountType> ELEMENT_TYPES = asList(PRIMARY_NAME, ALIAS, BIC,
            PASSPORT, ISIN);
    private static final List<AddModSupCountType> RECORD_TYPES = asList(PERSON, ENTITY, VESSEL, COUNTRY,
            CITY, AIRPORT, SEAPORT, OTHER);

    @Autowired
    @Lazy
    private AddModSupRepository repo;

    @Autowired
    private PaginationService paginationService;

    public List<AddModSupCountLine> getAddModSupElementsCounts(long versionId) {
        Map<Object, List<Object[]>> lines = repo.getAddModSupElementsCountsRaw(versionId).stream()
                .collect(groupingBy(r -> r[0]));

        return mapAddModSupCountLines(lines, ELEMENT_TYPES);
    }
    public AddModSupCountLine getAddModSupAliasCounts(long versionId) {
        final Function<Object[], Object> elementType = r -> r[0];
        Map<Object, List<Object[]>> lines = repo.getAddModSupAliasCountsRaw(versionId).stream()
                .collect(groupingBy(elementType));
        return mapAddModSupCountLines(lines, singletonList(ALIAS)).stream()
                .filter(el->ALIAS==el.getElementType())
                .findFirst()
                .orElse(AddModSupCountLine.createEmptyLine(AddModSupCountType.ALIAS));

    }
    public Map<AddModSupAction, Integer> getAddModSupDetailsElementsCounts(long versionId) {
        Map<AddModSupAction, Integer> results = Arrays.stream(AddModSupAction.values()).collect(toMap(a -> a, a -> 0));
        Consumer<Object[]> addToActionsCountResults = rawAddModSupCountResult -> {
            AddModSupAction action = AddModSupAction.fromStringAction((String) rawAddModSupCountResult[0]);
            Integer count = ((Number) rawAddModSupCountResult[1]).intValue();
            results.put(action, count);
        };
        repo.getAddModSupDetailsElementsCountsRaw(versionId).forEach(addToActionsCountResults);
        return results;
    }

    public List<AddModSupCountLine> getAddModSupRecordsCounts(long versionId) {
        Function<Object[], Object> recordType = r -> r[0];
        Map<Object, List<Object[]>> lines = repo.getAddModSupRecordsCountRaw(versionId).stream()
                .collect(groupingBy(recordType));

        return mapAddModSupCountLines(lines, RECORD_TYPES);
    }

    public int getCountModifiedRecordsForSynthesis(long versionId) {
        return repo.countModifiedRecordsForVersionSynthesis(versionId);
    }

    public Stream<AddModSupLine> getAddModSupElementsByAction(Long versionId, AddModSupAction action) {
        return action.queryAddModSupElementsDetails(repo, versionId);
    }

    public List<AddModSupLineForSynthesis> getPaginatedAddModSupElementsByAction(Long versionId, AddModSupAction action, PaginationOptions paginationOptions) {
        Sort defaultSort = Sort.by(Direction.ASC, "REGLISS_REFERENCE", "FIELD_TYPE_SORT_PRIORITY", "AKA_REFERENCE");

        Map<String, Object> userSortMapping = ImmutableMap.builder()
                .put("recordReglissReference", "REFERENCE_FOR_SORT")
                .put("fullName", "PRIMARY_NAME_FOR_SORT")
                .put("recordType", "TYPE_FOR_SORT")
                .put("modElement", "ELEMENT_FOR_SORT")
                .put("verificationStatus", "VERIFICATION_STATUS_FOR_SORT")
                .build();

        PageRequest pageRequest = paginationService.toPageRequestWithMultipleDefaultOrders(paginationOptions, userSortMapping, defaultSort);

        Map<String, Object> params = new HashMap<>();
        params.put("versionId", versionId);
        if(!action.equals(AddModSupAction.MOD)) {
            params.put("action", action.getActionName());
        }

        String sql = action.equals(AddModSupAction.MOD) ? AddModSupRepository.MOD_QUERY_BY_ACTION_WITH_VERIFICATION_INFO : AddModSupRepository.ADD_SUP_QUERY_BY_ACTION_WITH_VERIFICATION_INFO;

        Query query = paginationService.createNativePaginatedQuery(sql, params, pageRequest);

        log.debug("Launching add mod sup search query for action {}...", action);
        List<Object[]> resultsObjs = PerformanceUtil.runAndMeasure("AddModSupQuery ", query::getResultList);

        log.debug("AddModSupQuery returned {} items", resultsObjs.size());

        return resultsObjs.stream().map(
            AddModSupLineForSynthesis::new).collect(toList());
    }

    public Stream<AddModSupLine> getAddModSupElements(Long versionId) {
        return repo.getAddModSupElementsRaw(versionId).map(AddModSupLine::new);
    }


    public List<AddModSupLineForSynthesis> getAllAddModSupElementsByAction(Long versionId, AddModSupAction action) {

        Map<String, Object> params = new HashMap<>();
        params.put("versionId", versionId);
        if(!action.equals(AddModSupAction.MOD)) {
            params.put("action", action.getActionName());
        }

        String sql = action.equals(AddModSupAction.MOD) ? AddModSupRepository.MOD_QUERY_BY_ACTION_WITH_VERIFICATION_INFO : AddModSupRepository.ADD_SUP_QUERY_BY_ACTION_WITH_VERIFICATION_INFO;

        Query query = entityManager.createNativeQuery(sql);  // NOSONAR: used internally only
        for(Map.Entry<String, Object> pair : params.entrySet()){
            query.setParameter(pair.getKey(), pair.getValue());
        }

        log.debug("Launching add mod sup search query for action {}...", action);
        List<Object[]> resultsObjs = PerformanceUtil.runAndMeasure("AllAddModSupQuery ", query::getResultList);
        log.debug("AllAddModSupQuery returned {} items", resultsObjs.size());

        return resultsObjs.stream().map(
                AddModSupLineForSynthesis::new).collect(toList());
    }

    private List<AddModSupCountLine> mapAddModSupCountLines(Map<Object, List<Object[]>> lines, List<AddModSupCountType> countTypes) {
        List<AddModSupCountLine> dbCounts = lines.values().stream()
                .map(AddModSupCountLine::new)
                .collect(toList());

        List<AddModSupCountLine> result = new ArrayList<>(
                getOrEmpty(dbCounts, countTypes, AddModSupCountLine::createEmptyLine)
        );
        result.add(AddModSupCountLine.createTotalsLine(result));
        return result;
    }

    private <T extends AddModSupCountLine> List<T> getOrEmpty(List<T> lines, List<AddModSupCountType> elementTypes, Function<AddModSupCountType, T> createEmptyLine) {
        List<T> result = new ArrayList<>();

        for (AddModSupCountType elementType : elementTypes) {
            T elementTypeCounts = lines.stream()
                    .filter(line -> line.isOfType(elementType))
                    .findFirst().orElse(createEmptyLine.apply(elementType));
            result.add(elementTypeCounts);
        }

        return result;
    }
}
