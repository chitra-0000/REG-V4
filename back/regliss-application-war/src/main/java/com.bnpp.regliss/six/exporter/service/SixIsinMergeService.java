package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.six.entity.FilteredInstrumentFile;
import com.bnpp.regliss.entity.six.entity.FilteredStructuredFile;
import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.SixSubFilters;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.six.SixFiltersRepository;
import com.bnpp.regliss.six.importer.mapper.FilteredDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

@Service
@ReglissBatchProfile
@Slf4j
public class SixIsinMergeService {

    private static final String LIST_ID = "listId";
    private static final String VERSION_ID = "versionId";
    private static final String STRUCT_LIST_ID = "structListId";
    private static final String STRUCT_VERSION_ID = "structVersionId";
    private static final String CONFIDENCE = "TO_NUMBER(i.CONFIDENCE_LEVEL DEFAULT NULL ON CONVERSION ERROR)";

    private static final String ACTIVE_INSTRUMENT_ISINS = "SELECT " + SixFileKind.INSTRUMENT.joinHint() + " DISTINCT i.ISIN"
            + " FROM SIX_INSTRUMENTS i JOIN SIX_TARGET t ON i.ID = t.SIX_INSTRU_ID"
            + " WHERE i.LIST_ID = :" + LIST_ID + " AND " + "i.VERSION_ID = :" + VERSION_ID + " AND TRIM(i.ISIN) IS NOT NULL"
            + " AND UPPER(TRIM(i.ACTIVE_FLAG)) <> 'INACTIVE'"
            + " AND ((UPPER(TRIM(t.TARGET)) = 'CMIC' AND " + CONFIDENCE + " > 0)"
            + " OR (UPPER(TRIM(t.TARGET)) <> 'CMIC' AND UPPER(TRIM(t.SANCTIONED)) = 'YES' AND " + CONFIDENCE + " > 25))";

    private static final String INSTRUMENT_ISINS = "SELECT /*+ INDEX(i " + SixFileKind.INSTRUMENT.getVersionIndex() + ") */"
            + " i.ISIN AS six_isin FROM SIX_INSTRUMENTS i WHERE i.LIST_ID = :" + LIST_ID + " AND i.VERSION_ID = :" + VERSION_ID
            + " AND TRIM(i.ISIN) IS NOT NULL";
    private static final String STRUCTURED_ISINS = "SELECT /*+ INDEX(i " + SixFileKind.STRUCTURED.getVersionIndex() + ") */"
            + " i.HOST_ISIN AS six_isin FROM SIX_STRUCTURED i WHERE i.LIST_ID = :" + STRUCT_LIST_ID
            + " AND i.VERSION_ID = :" + STRUCT_VERSION_ID + " AND TRIM(i.HOST_ISIN) IS NOT NULL";

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private SixFileFilterService sixFileFilterService;
    @Autowired
    private SixFiltersRepository sixFiltersRepository;
    @Autowired
    private FilteredDataMapper filteredDataMapper;

    @Value("${six.cmic.list}")
    private String sixCMICList;

    @Value("${six.e014071.list}")
    private String sixE014071List;

    private VersionData lastVersionData;

    public static final class Scope {
        private final long listId;
        private final long versionId;

        private Scope(long listId, long versionId) {
            this.listId = listId;
            this.versionId = versionId;
        }

        public static Scope of(Long listId, Long versionId) {
            return listId == null || versionId == null ? null : new Scope(listId, versionId);
        }

        @Override
        public String toString() {
            return "list " + listId + " version " + versionId;
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SixIsinMergeContent load(ReglissList outputList, Scope instrument, Scope structured, Set<String> outputIsins) {
        if (instrument == null && structured == null) {
            return SixIsinMergeContent.none();
        }
        long start = System.currentTimeMillis();
        VersionData data = versionData(instrument, structured);
        Set<String> candidates = new LinkedHashSet<>(outputIsins);
        candidates.retainAll(data.duplicateIsins);
        if (candidates.isEmpty()) {
            log.info("SIX same-ISIN merge, list {}: nothing to merge ({} active instrument ISINs)", outputList.getReference(),
                    data.activeInstrumentIsins.size());
            return new SixIsinMergeContent(data.activeInstrumentIsins, Collections.emptyMap(), Collections.emptyMap());
        }
        List<SixFilters> filters = filtersOf(outputList);
        String reference = outputList.getReference();

        Map<String, List<FilteredInstrumentFile>> instrumentRows = Collections.emptyMap();
        if (instrument != null) {
            instrumentRows = byIsin(sixFileFilterService.instrumentContentRows(filters, data.instrumentExclusions, candidates),
                    row -> filteredDataMapper.filteredInstrumentFile(row, reference), FilteredInstrumentFile::getIsin);
        }
        Map<String, List<FilteredStructuredFile>> structuredRows = Collections.emptyMap();
        Set<String> structuredCandidates = new LinkedHashSet<>(candidates);
        structuredCandidates.removeAll(data.activeInstrumentIsins);
        if (structured != null && !structuredCandidates.isEmpty()) {
            structuredRows = byIsin(sixFileFilterService.structuredContentRows(filters, data.structuredExclusions, structuredCandidates),
                    row -> filteredDataMapper.filteredStructuredFile(row, reference), FilteredStructuredFile::getHostIsin);
        }
        SixIsinMergeContent content = new SixIsinMergeContent(data.activeInstrumentIsins, instrumentRows, structuredRows);
        log.info("SIX same-ISIN merge, list {}: {} ISINs with several rows, {} with rows to merge, {} active instrument ISINs,"
                        + " read in {} ms", reference, candidates.size(), content.mergedIsinCount(), content.activeInstrumentCount(),
                System.currentTimeMillis() - start);
        return content;
    }

    public synchronized void startRun() {
        lastVersionData = null;
    }

    private synchronized VersionData versionData(Scope instrument, Scope structured) {
        String key = instrument + " / " + structured;
        if (lastVersionData == null || !lastVersionData.key.equals(key)) {
            lastVersionData = new VersionData(key, instrument, structured);
        }
        return lastVersionData;
    }

    private final class VersionData {
        final String key;
        final Set<String> activeInstrumentIsins;
        final Set<String> duplicateIsins;
        final SixFileFilterService.Exclusions instrumentExclusions;
        final SixFileFilterService.Exclusions structuredExclusions;

        VersionData(String key, Scope instrument, Scope structured) {
            this.key = key;
            this.activeInstrumentIsins = instrument == null ? Collections.emptySet() : activeInstrumentIsins(instrument);
            this.duplicateIsins = duplicateIsins(instrument, structured);
            List<SixFilters> cmic = loaded(sixFiltersRepository.getFiltersByListRefAndActiveTrueAndDeletedFalse(sixCMICList));
            List<SixFilters> e014071 = loaded(sixFiltersRepository.getFiltersByListRefAndActiveTrueAndDeletedFalse(sixE014071List));
            this.instrumentExclusions = instrument == null ? null
                    : sixFileFilterService.exclusionsFor(SixFileKind.INSTRUMENT, instrument.listId, instrument.versionId, cmic, e014071);
            this.structuredExclusions = structured == null ? null
                    : sixFileFilterService.exclusionsFor(SixFileKind.STRUCTURED, structured.listId, structured.versionId, cmic, e014071);
            log.info("SIX same-ISIN merge, raw versions {}: {} active instrument ISINs, {} ISINs with several rows", key,
                    activeInstrumentIsins.size(), duplicateIsins.size());
        }
        private Set<String> activeInstrumentIsins(Scope instrument) {
            MapSqlParameterSource params = new MapSqlParameterSource(LIST_ID, instrument.listId).addValue(VERSION_ID, instrument.versionId);
            return new HashSet<>(jdbcTemplate.queryForList(ACTIVE_INSTRUMENT_ISINS, params, String.class));
        }

        private Set<String> duplicateIsins(Scope instrument, Scope structured) {
            MapSqlParameterSource params = new MapSqlParameterSource();
            List<String> parts = new ArrayList<>();
            if (instrument != null) {
                parts.add(INSTRUMENT_ISINS);
                params.addValue(LIST_ID, instrument.listId).addValue(VERSION_ID, instrument.versionId);
            }
            if (structured != null) {
                parts.add(STRUCTURED_ISINS);
                params.addValue(STRUCT_LIST_ID, structured.listId).addValue(STRUCT_VERSION_ID, structured.versionId);
            }
            String sql = "SELECT u.six_isin FROM (" + String.join(" UNION ALL ", parts) + ") u GROUP BY u.six_isin HAVING COUNT(*) > 1";
            return new HashSet<>(jdbcTemplate.queryForList(sql, params, String.class));
        }
    }

    private List<SixFilters> filtersOf(ReglissList outputList) {
        return loaded(sixFiltersRepository.getFiltersByListIdAndActiveTrueAndDeletedFalse(outputList.getId()));
    }

    private static List<SixFilters> loaded(List<SixFilters> filters) {
        if (filters == null) {
            return Collections.emptyList();
        }
        filters.forEach(f -> f.getActiveSixSubfilters().forEach(SixSubFilters::getActiveSixSubORfilters));
        return filters;
    }

    private static <R, F> Map<String, List<F>> byIsin(List<R> rows, Function<R, F> mapper, Function<F, String> isinOf) {
        Map<String, List<F>> byIsin = new LinkedHashMap<>();
        for (R row : rows) {
            F mapped = mapper.apply(row);
            String isin = isinOf.apply(mapped);
            if (Objects.nonNull(isin)) {
                byIsin.computeIfAbsent(isin, k -> new ArrayList<>()).add(mapped);
            }
        }
        return byIsin;
    }
}


