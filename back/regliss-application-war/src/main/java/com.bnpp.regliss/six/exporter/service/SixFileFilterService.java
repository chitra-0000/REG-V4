package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.entity.six.entity.InstrumentFile;
import com.bnpp.regliss.entity.six.entity.OptionsFile;
import com.bnpp.regliss.entity.six.entity.SixFilters;
import com.bnpp.regliss.entity.six.entity.StructuredFile;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.six.importer.extractor.InstrumentFileExtractor;
import com.bnpp.regliss.six.importer.extractor.OptionsFileExtractor;
import com.bnpp.regliss.six.importer.extractor.StructuredFileExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Service
@ReglissBatchProfile
@Slf4j
public class SixFileFilterService {

    private static final String DATA_EXTRACTOR_1 = "Data extractor 1";
    private static final String DATA_EXTRACTOR_2 = "Data extractor 2";
    private static final String LIST_ID = "listId";
    private static final String VERSION_ID = "versionId";
    private static final String AND = " AND ";
    private static final String MANDATORY_SELECT = " WHERE i.LIST_ID = :listId AND i.VERSION_ID = :versionId";
    private static final String SELECT = "SELECT ";
    private static final String RAW_ALIAS = "i";
    private static final String TARGET_ALIAS = "t";
    private static final String MERGE_ISINS = "mergeIsins";
    private static final int MERGE_ISINS_PER_QUERY = 1000;

    private static final String[] ROW_KEYS = {"ID", "LIST_ID", "VERSION_ID"};
    private static final String[] SHARED_COLUMNS = {"FISN", "DATE_OPENED_IN_SIX", "INSTRUMENT_TYPE", "DENOMINATION_CURRENCY",
            "ACTIVE_FLAG", "ISSUE_DATE", "CONFIDENCE_LEVEL", "REGIMES"};
    private static final String[] IDENTIFIER_COLUMNS = {"SEDOL", "CUSIP", "CINS", "AUSTRIAN", "BELGIAN", "CANADIAN", "GERMAN",
            "DENMARK", "FRANCE_RGA", "FRANCE_EUROCLEAR", "ITALIAN", "JAPANESE_CURRENT", "JAPANESE_NEW", "LUXEMBOURG",
            "NETHERLAND", "NORWEGIAN", "SWEDISH", "XS_INT_NUMBER", "PORTUGAL", "SOUTH_KOREA", "HONG_KONG", "FIGI_GLOBAL_ID",
            "FIGI_GLOBAL_SHARE_CLASS_ID"};
    private static final String[] INSTRUMENT_OWN_COLUMNS = {"RECORD_ORIGIN", "OLS_YES", "OLS_NO", "LINK_ENTITY", "LINK_CSID",
            "SANCTIONED_PARENT_ENTITY", "NAME_DIRECT_ISSUER", "ISIN", "INSTR_NAME", "CH_VALOR", "INDICATIVE_ISSUE_DATE",
            "SANCTION_RELEVANT_ASSET_CLASS", "MAIN_INSTRUMENT", "EQUITY_TYPE_OF_ISSUANCE", "MATURITY_DATE",
            "DEBT_LIFETIME_IN_DAYS", "SUBSTITUTE_ISSUE_DATE", "CAPITAL_CHANGE_DATE", "OBLIGOR_ROLE", "UNDERLYING_OBLIGOR",
            "UNDERLYING_OBLIGOR_GK", "REFERENCE_CAPITAL", "ACTUAL_CAPITAL"};
    private static final String[] STRUCTURED_OWN_COLUMNS = {"HOST_CH", "HOST_ISIN", "HOST_GK", "HOST_ISSUER_SHORTNAME",
            "DESCRIPTION", "SUBSTITUTE_ISSUE_DATE", "INDICATIVE_ISSUE_DATE", "SETTLEMENT_STYLE", "MATURITY_DATE",
            "UNDERLYING_CH", "UNDERLYING_ISIN", "UNDERLYING_GK", "UNDERLYING_ISSUER_SHORTNAME"};
    private static final String[] OPTION_OWN_COLUMNS = {"CH_OPTION", "ISIN_OPTION", "DESCRIPTION", "ISSUER_GK", "ISSUER_NAME",
            "EXPIRY_DATE", "UNDERLYING_CH", "UNDERLYING_ISIN", "UNDERLYING_ISSUER_GK", "UNDERLYING_ISSUER_NAME"};
    private static final String[] INSTRUMENT_TARGET_KEYS = {"ID", "SIX_INSTRU_ID"};
    private static final String[] OTHER_TARGET_KEYS = {"SIX_STRUCT_ID", "SIX_OPT_ID"};
    private static final String[] TARGET_VALUE_COLUMNS = {"TARGET", "REGIME", "LEGAL_BASIS", "SANCTIONED", "STATUS",
            "SANCTION_FLAG_CHANGED", "REASON_FOR_CHANGE", "SANCTIONS_RATIONALE"};

    private static final String INSTR_COLUMNS = selectList(RAW_ALIAS, ROW_KEYS, INSTRUMENT_OWN_COLUMNS, SHARED_COLUMNS,
            IDENTIFIER_COLUMNS) + ", " + selectList(TARGET_ALIAS, INSTRUMENT_TARGET_KEYS, TARGET_VALUE_COLUMNS);
    private static final String STRUCT_COLUMNS = selectList(RAW_ALIAS, ROW_KEYS, STRUCTURED_OWN_COLUMNS, SHARED_COLUMNS,
            IDENTIFIER_COLUMNS) + ", " + selectList(TARGET_ALIAS, INSTRUMENT_TARGET_KEYS, OTHER_TARGET_KEYS, TARGET_VALUE_COLUMNS);
    private static final String OPTION_COLUMNS = selectList(RAW_ALIAS, ROW_KEYS, OPTION_OWN_COLUMNS, SHARED_COLUMNS,
            IDENTIFIER_COLUMNS) + ", " + selectList(TARGET_ALIAS, INSTRUMENT_TARGET_KEYS, OTHER_TARGET_KEYS, TARGET_VALUE_COLUMNS);

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${six.export.filter.fetch.size:500}")
    private int filterFetchSize;

    private NamedParameterJdbcTemplate readTemplate;

    @PostConstruct
    void initReadTemplate() {
        JdbcTemplate shared = jdbcTemplate.getJdbcTemplate();
        DataSource dataSource = shared.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("SIX filter reads: the shared JdbcTemplate has no DataSource");
        }
        JdbcTemplate reads = new JdbcTemplate(dataSource);
        reads.setExceptionTranslator(shared.getExceptionTranslator());
        reads.setQueryTimeout(shared.getQueryTimeout());
        reads.setMaxRows(shared.getMaxRows());
        reads.setFetchSize(Math.max(1, filterFetchSize));
        readTemplate = new NamedParameterJdbcTemplate(reads);
        log.info("SIX filter reads: fetch size {}", reads.getFetchSize());
    }

    @Autowired
    private DynamicSqlBuilder sqlBuilder;

    public Exclusions exclusionsFor(SixFileKind kind, long rawListId, long versionId,
                                    List<SixFilters> cmicSixFilters, List<SixFilters> e014071SixFilters) {
        return new Exclusions(kind, rawListId, versionId, cmicSixFilters, e014071SixFilters);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Exclusions prepareExclusions(SixFileKind kind, long rawListId, long versionId, List<SixFilters> cmicSixFilters,
                                        List<SixFilters> e014071SixFilters, Collection<List<SixFilters>> filtersOfEveryList) {
        Exclusions exclusions = new Exclusions(kind, rawListId, versionId, cmicSixFilters, e014071SixFilters);
        boolean cmic = false;
        boolean e014071 = false;
        for (List<SixFilters> listFilters : filtersOfEveryList) {
            for (SixFilters filter : listFilters) {
                if (appliesTo(kind, filter.getFileType())) {
                    cmic |= filter.isExcludeCMIC();
                    e014071 |= filter.isExcludeE014071();
                }
            }
        }
        if (cmic) {
            exclusions.cmicIds();
        }
        if (e014071) {
            exclusions.e014071Ids();
        }
        return exclusions;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<InstrumentFile> filterInstrument(List<SixFilters> sixFilters, Exclusions exclusions) {
        return filter(SixFileKind.INSTRUMENT, sixFilters, exclusions, new InstrumentFileExtractor(),
                InstrumentFile::getId, InstrumentFile::getIsin);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<StructuredFile> filterStructured(List<SixFilters> sixFilters, Exclusions exclusions) {
        return filter(SixFileKind.STRUCTURED, sixFilters, exclusions, new StructuredFileExtractor(),
                StructuredFile::getId, StructuredFile::getHostIsin);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<OptionsFile> filterOptions(List<SixFilters> sixFilters, Exclusions exclusions) {
        return filter(SixFileKind.OPTIONS, sixFilters, exclusions, new OptionsFileExtractor(),
                OptionsFile::getId, OptionsFile::getIsinOption);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<Object> filter(SixFileKind kind, List<SixFilters> sixFilters, Exclusions exclusions) {
        switch (kind) {
            case INSTRUMENT: return Collections.unmodifiableList(filterInstrument(sixFilters, exclusions));
            case STRUCTURED: return Collections.unmodifiableList(filterStructured(sixFilters, exclusions));
            case OPTIONS:    return Collections.unmodifiableList(filterOptions(sixFilters, exclusions));
            default:         throw new IllegalArgumentException("Unsupported SIX file type " + kind);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<InstrumentFile> instrumentContentRows(List<SixFilters> sixFilters, Exclusions exclusions, Collection<String> isins) {
        return contentRows(SixFileKind.INSTRUMENT, sixFilters, exclusions, isins, new InstrumentFileExtractor(), InstrumentFile::getId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<StructuredFile> structuredContentRows(List<SixFilters> sixFilters, Exclusions exclusions, Collection<String> isins) {
        return contentRows(SixFileKind.STRUCTURED, sixFilters, exclusions, isins, new StructuredFileExtractor(), StructuredFile::getId);
    }

    private <R> List<R> contentRows(SixFileKind kind, List<SixFilters> sixFilters, Exclusions exclusions, Collection<String> isins,
                                    ResultSetExtractor<List<R>> extractor, ToLongFunction<R> idOf) {
        if (exclusions.kind != kind) {
            throw new IllegalArgumentException("Exclusions of " + exclusions.kind + " used for " + kind);
        }
        List<String> isinList = new ArrayList<>(new LinkedHashSet<>(isins));
        if (isinList.isEmpty()) {
            return new ArrayList<>();
        }
        MapSqlParameterSource params = scopeParams(exclusions.rawListId, exclusions.versionId);
        List<String> clauses = new ArrayList<>();
        boolean excludeCmic = false;
        boolean excludeE014071 = false;
        for (SixFilters sixFilter : sixFilters) {
            if (appliesTo(kind, sixFilter.getFileType())) {
                clauses.add(sqlBuilder.buildBaseWhereClause(sixFilter.getActiveSixSubfilters(), params, kind.getRootIsinColumn()));
                excludeCmic |= sixFilter.isExcludeCMIC();
                excludeE014071 |= sixFilter.isExcludeE014071();
            }
        }
        String where = combineJoin(clauses);
        String sql = includeSelect(kind, params) + (where.isEmpty() ? "" : AND + where)
                + AND + kind.getRootIsinColumn() + " IN (:" + MERGE_ISINS + ") ORDER BY i.ID, t.ID";
        log.info("{} same-ISIN content sql - {}", kind, sql);

        long start = System.currentTimeMillis();
        List<R> rows = new ArrayList<>();
        for (int from = 0; from < isinList.size(); from += MERGE_ISINS_PER_QUERY) {
            params.addValue(MERGE_ISINS, isinList.subList(from, Math.min(from + MERGE_ISINS_PER_QUERY, isinList.size())));
            List<R> part = readTemplate.query(sql, params, extractor);
            if (part != null) {
                rows.addAll(part);
            }
        }
        Set<Long> excludedIds = exclusions.idsToRemove(excludeCmic, excludeE014071);
        rows.removeIf(row -> excludedIds.contains(idOf.applyAsLong(row)));
        log.info("{} same-ISIN content: {} ISINs, {} rows in {} ms", kind, isinList.size(), rows.size(),
                System.currentTimeMillis() - start);
        return rows;
    }

    private <R> List<R> filter(SixFileKind kind, List<SixFilters> sixFilters, Exclusions exclusions,
                               ResultSetExtractor<List<R>> extractor, ToLongFunction<R> idOf, Function<R, String> isinOf) {
        if (exclusions.kind != kind) {
            throw new IllegalArgumentException("Exclusions of " + exclusions.kind + " used for " + kind);
        }
        MapSqlParameterSource params = scopeParams(exclusions.rawListId, exclusions.versionId);
        List<String> clauses = new ArrayList<>();
        Set<String> emptyInAllTargets = new HashSet<>();
        Set<String> containsInAllTargets = new HashSet<>();
        boolean excludeCmic = false;
        boolean excludeE014071 = false;

        for (SixFilters sixFilter : sixFilters) {
            if (appliesTo(kind, sixFilter.getFileType())) {
                clauses.add(sqlBuilder.buildWhereClause(sixFilter.getActiveSixSubfilters(), params, subqueryBase(kind),
                        kind.getRootIsinColumn(), emptyInAllTargets, containsInAllTargets));
                excludeCmic |= sixFilter.isExcludeCMIC();
                excludeE014071 |= sixFilter.isExcludeE014071();
            }
        }

        String where = combineJoin(clauses);
        String includeSql = includeSelect(kind, params) + (where.isEmpty() ? "" : AND + where);
        log.info("{} include sql - {}", kind, includeSql);
        log.debug("{} include params - {}", kind, params);

        long start = System.currentTimeMillis();
        List<R> included = readTemplate.query(includeSql, params, extractor);
        if (included == null) {
            return Collections.emptyList();
        }
        int read = included.size();

        Set<Long> excludedIds = exclusions.idsToRemove(excludeCmic, excludeE014071);
        included.removeIf(row -> emptyInAllTargets.contains(isinOf.apply(row))
                || containsInAllTargets.contains(isinOf.apply(row))
                || excludedIds.contains(idOf.applyAsLong(row)));

        log.info("{} filter: {} rows read, {} kept ({} 'all targets' ISINs, CMIC excluded: {}, E014071 excluded: {}) in {} ms",
                kind, read, included.size(), emptyInAllTargets.size() + containsInAllTargets.size(),
                excludeCmic, excludeE014071, System.currentTimeMillis() - start);
        return included;
    }

    private static String fromClause(SixFileKind kind) {
        return " FROM " + kind.getTable() + " i JOIN SIX_TARGET t ON i.ID = t." + kind.getTargetFkColumn() + MANDATORY_SELECT;
    }

    private static String fromClause(SixFileKind kind, MapSqlParameterSource params) {
        if (!DynamicSqlBuilder.usesExclusionJoin(params)) {
            return fromClause(kind);
        }
        return " FROM " + kind.getTable() + " i JOIN SIX_TARGET t ON i.ID = t." + kind.getTargetFkColumn()
                + DynamicSqlBuilder.exclusionJoin(kind) + MANDATORY_SELECT;
    }

    private static String joinHint(SixFileKind kind, MapSqlParameterSource params) {
        return DynamicSqlBuilder.usesExclusionJoin(params) ? kind.joinHintWithExclusion() : kind.joinHint();
    }

    private static String includeSelect(SixFileKind kind, MapSqlParameterSource params) {
        return SELECT + joinHint(kind, params) + columns(kind) + fromClause(kind, params);
    }

    private static String subqueryBase(SixFileKind kind) {
        return SELECT + kind.joinHint() + " DISTINCT " + kind.getRootIsinColumn() + fromClause(kind);
    }

    private static String selectList(String alias, String[]... groups) {
        return " " + Arrays.stream(groups).flatMap(Arrays::stream)
                .map(column -> alias + "." + column + " AS " + alias + "_" + column.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", ")) + " ";
    }

    private static String columns(SixFileKind kind) {
        switch (kind) {
            case INSTRUMENT: return INSTR_COLUMNS;
            case STRUCTURED: return STRUCT_COLUMNS;
            case OPTIONS:    return OPTION_COLUMNS;
            default:         throw new IllegalArgumentException("Unsupported SIX file type " + kind);
        }
    }

    private static boolean appliesTo(SixFileKind kind, String fileType) {
        return kind.getFilterFileType().equalsIgnoreCase(fileType)
                || DATA_EXTRACTOR_1.equalsIgnoreCase(fileType)
                || DATA_EXTRACTOR_2.equalsIgnoreCase(fileType);
    }

    private static String combineJoin(List<String> clauses) {
        List<String> notBlank = new ArrayList<>();
        for (String clause : clauses) {
            if (clause != null && !clause.trim().isEmpty()) {
                notBlank.add(clause);
            }
        }
        return String.join(AND, notBlank);
    }

    private static MapSqlParameterSource scopeParams(long rawListId, long versionId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(LIST_ID, rawListId);
        params.addValue(VERSION_ID, versionId);
        return params;
    }

    public final class Exclusions {
        private final SixFileKind kind;
        private final long rawListId;
        private final long versionId;
        private final List<SixFilters> cmicSixFilters;
        private final List<SixFilters> e014071SixFilters;
        private Set<Long> cmicIds;
        private Set<Long> e014071Ids;

        private Exclusions(SixFileKind kind, long rawListId, long versionId,
                           List<SixFilters> cmicSixFilters, List<SixFilters> e014071SixFilters) {
            this.kind = kind;
            this.rawListId = rawListId;
            this.versionId = versionId;
            this.cmicSixFilters = cmicSixFilters == null ? Collections.emptyList() : cmicSixFilters;
            this.e014071SixFilters = e014071SixFilters == null ? Collections.emptyList() : e014071SixFilters;
        }

        synchronized Set<Long> cmicIds() {
            if (cmicIds == null) {
                cmicIds = excludedIds(cmicSixFilters, "cmic");
            }
            return cmicIds;
        }

        synchronized Set<Long> e014071Ids() {
            if (e014071Ids == null) {
                e014071Ids = excludedIds(e014071SixFilters, "eo14071");
            }
            return e014071Ids;
        }

        Set<Long> idsToRemove(boolean cmic, boolean e014071) {
            Set<Long> ids = new HashSet<>();
            if (cmic) {
                ids.addAll(cmicIds());
            }
            if (e014071) {
                ids.addAll(e014071Ids());
            }
            return ids;
        }

        private Set<Long> excludedIds(List<SixFilters> exclusionFilters, String label) {
            MapSqlParameterSource params = scopeParams(rawListId, versionId);
            List<String> clauses = new ArrayList<>();
            Set<String> emptyInAllTargets = new HashSet<>();
            Set<String> containsInAllTargets = new HashSet<>();
            for (SixFilters filter : exclusionFilters) {
                if (appliesTo(kind, filter.getFileType())) {
                    clauses.add(sqlBuilder.buildWhereClause(filter.getActiveSixSubfilters(), params, subqueryBase(kind),
                            kind.getRootIsinColumn(), emptyInAllTargets, containsInAllTargets));
                }
            }
            String where = combineJoin(clauses);
            if (where.isEmpty()) {
                log.info("{} exclude {}: no condition for this file type, nothing excluded", kind, label);
                return Collections.emptySet();
            }
            String sql = SELECT + joinHint(kind, params) + " i.ID AS i_id, " + kind.getRootIsinColumn() + " AS i_isin"
                    + fromClause(kind, params) + AND + where;
            log.info("{} exclude {} sql - {}", kind, label, sql);
            log.debug("{} exclude {} params - {}", kind, label, params);

            long start = System.currentTimeMillis();
            Set<Long> ids = new HashSet<>();
            readTemplate.query(sql, params, rs -> {
                String isin = rs.getString("i_isin");
                if (!emptyInAllTargets.contains(isin) && !containsInAllTargets.contains(isin)) {
                    ids.add(rs.getLong("i_id"));
                }
            });
            log.info("{} exclude {}: {} rows in {} ms (computed once, used for every list)", kind, label, ids.size(),
                    System.currentTimeMillis() - start);
            return ids;
        }

        public SixFileKind getKind() {
            return kind;
        }
    }
}
