package com.bnpp.regliss.six.exporter.service;

import com.bnpp.regliss.ReglissException;
import com.bnpp.regliss.entity.six.entity.*;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

@Component
@ReglissBatchProfile
public class DynamicSqlBuilder {

    private static final Map<String, String> OPERATOR_SQL = new HashMap<>();
    private static final Map<String, String> FIELD_TO_COLUMN = new HashMap<>();
    private static final String NULL = "IS NULL";
    private static final String NOT_NULL = "IS NOT NULL";
    private static final String LEGAL_BASIS = "LEGAL_BASIS";
    private static final String REGIME = "REGIME";
    private static final String REGIME_FIELD = " - REGIME";
    private static final String EQUALS = "equals";
    private static final String NOT_EQUALS = "not equals";
    private static final String IS_EMPTY = "is empty";
    private static final String IS_NOT_EMPTY = "is not empty";
    private static final String CONTAINS = "contains";
    private static final String NON_CONTAINS = "not contains";
    private static final String STARTS_WITH = "starts with";
    private static final String NOT_STARTS_WITH = "not starts with";
    private static final String AND = " AND ";
    private static final String OR = " OR ";
    private static final String NOT_IN = " NOT IN ";
    private static final String EMPTY_IN_ALL_TARGETS = "empty in all targets";
    private static final String CONTAINS_IN_ALL_TARGETS = "contains in all targets";

    private static final String EXCLUSION_LIST_PARAM = "sixExclusionList";
    private static final String EXCLUSION_CHECKED_KEY = "sixExclusionListChecked";
    private static final String EXCLUSION_ALIAS = "ex";
    private static final String EXCLUSION_ISINS = "SELECT ci.code AS code FROM code_isin ci"
            + " JOIN record_isin ri ON ci.id = ri.isin_id"
            + " JOIN record r ON ri.record_id = r.id"
            + " JOIN regliss_list l ON r.list_id = l.id"
            + " JOIN version v ON v.list_id = l.id"
            + " WHERE l.reference = :" + EXCLUSION_LIST_PARAM
            + " AND TRIM(ci.code) IS NOT NULL"
            + " AND v.id = (SELECT MAX(vv.id) FROM version vv WHERE vv.list_id = l.id AND vv.status <> 'DELETED')"
            + " AND r.from_version <= v.id AND r.to_version >= v.id";

    private static final String ALL_TARGETS_INSTR = "SELECT " + SixFileKind.INSTRUMENT.joinHint()
            + " i.ISIN AS six_isin FROM SIX_INSTRUMENTS i JOIN SIX_TARGET t ON i.ID = t.SIX_INSTRU_ID "
            + " WHERE i.VERSION_ID = (SELECT MAX(j.VERSION_ID) FROM SIX_INSTRUMENTS j)"
            + " AND TRIM(i.isin) IS NOT NULL ";
    private static final String ALL_TARGETS_STRUCT = " UNION SELECT " + SixFileKind.STRUCTURED.joinHint()
            + " i.HOST_ISIN FROM SIX_STRUCTURED i JOIN SIX_TARGET t ON i.ID = t.SIX_STRUCT_ID "
            + " WHERE i.VERSION_ID = (SELECT MAX(j.VERSION_ID) FROM SIX_STRUCTURED j)"
            + " AND TRIM(i.host_isin) IS NOT NULL ";
    private static final String ALL_TARGETS_OPT = " UNION SELECT " + SixFileKind.OPTIONS.joinHint()
            + " i.ISIN_OPTION FROM SIX_OPTION i JOIN SIX_TARGET t ON i.ID = t.SIX_OPT_ID "
            + " WHERE i.VERSION_ID = (SELECT MAX(j.VERSION_ID) FROM SIX_OPTION j)"
            + " AND TRIM(i.isin_option) IS NOT NULL ";

    private static final Map<SixFileKind, Set<String>> KIND_COLUMNS = new EnumMap<>(SixFileKind.class);
    private static final Set<String> TARGET_COLUMNS;

    static {
        KIND_COLUMNS.put(SixFileKind.INSTRUMENT, columnNames(InstrumentFile.class));
        KIND_COLUMNS.put(SixFileKind.STRUCTURED, columnNames(StructuredFile.class));
        KIND_COLUMNS.put(SixFileKind.OPTIONS, columnNames(OptionsFile.class));
        TARGET_COLUMNS = columnNames(SixTarget.class);
    }

    private static Set<String> columnNames(Class<?> entity) {
        Set<String> names = new HashSet<>();
        for (Class<?> c = entity; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Column column = f.getAnnotation(Column.class);
                if (column != null && !column.name().isEmpty()) {
                    names.add(column.name().replace("\"", "").toUpperCase(Locale.ROOT));
                }
            }
        }
        return Collections.unmodifiableSet(names);
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${six.exclusion.list}")
    private String sixExclusionList;

    static {
        OPERATOR_SQL.put(EQUALS,             "= :%s");
        OPERATOR_SQL.put(NOT_EQUALS,         "<> :%s");
        OPERATOR_SQL.put(IS_EMPTY,           NULL);
        OPERATOR_SQL.put(IS_NOT_EMPTY,       NOT_NULL);
        OPERATOR_SQL.put("less than",        "< :%s");
        OPERATOR_SQL.put("greater than",     "> :%s");
        OPERATOR_SQL.put("less or equal",    "<= :%s");
        OPERATOR_SQL.put("greater or equal", ">= :%s");
        OPERATOR_SQL.put(CONTAINS,           "LIKE :%s");
        OPERATOR_SQL.put(NON_CONTAINS,       "NOT LIKE :%s");
        OPERATOR_SQL.put(STARTS_WITH,        "LIKE :%s");
        OPERATOR_SQL.put(NOT_STARTS_WITH,    "NOT LIKE :%s");
        OPERATOR_SQL.put("IN",               "IN (:%s)");
        OPERATOR_SQL.put(NOT_IN,             "NOT IN (:%s)");
    }

    static {
        FIELD_TO_COLUMN.put("LIST_ID", "i.list_id");
        FIELD_TO_COLUMN.put("VERSION_ID", "i.version_id");
        FIELD_TO_COLUMN.put("RECORD_ORIGIN", "i.record_origin");
        FIELD_TO_COLUMN.put("OLS_YES", "i.ols_yes");
        FIELD_TO_COLUMN.put("OLS_NO", "i.ols_no");
        FIELD_TO_COLUMN.put("LINK_ENTITY", "i.link_entity");
        FIELD_TO_COLUMN.put("LINK_CSID", "i.link_csid");
        FIELD_TO_COLUMN.put("SANCTIONED_PARENT_ENTITY", "i.sanctioned_parent_entity");
        FIELD_TO_COLUMN.put("NAME_DIRECT_ISSUER", "i.name_direct_issuer");
        FIELD_TO_COLUMN.put("CONFIDENCE_LEVEL", "i.confidence_level");
        FIELD_TO_COLUMN.put("ISIN", "i.isin");
        FIELD_TO_COLUMN.put("INSTR_NAME", "i.instr_name");
        FIELD_TO_COLUMN.put("FISN", "i.fisn");
        FIELD_TO_COLUMN.put("CH_VALOR", "i.ch_valor");
        FIELD_TO_COLUMN.put("INDICATIVE_ISSUE_DATE", "i.indicative_issue_date");
        FIELD_TO_COLUMN.put("INSTRUMENT_TYPE", "i.instrument_type");
        FIELD_TO_COLUMN.put("SANCTION_RELEVANT_ASSET_CLASS", "i.sanction_relevant_asset_class");
        FIELD_TO_COLUMN.put("MAIN_INSTRUMENT", "i.main_instrument");
        FIELD_TO_COLUMN.put("EQUITY_TYPE_OF_ISSUANCE", "i.equity_type_of_issuance");
        FIELD_TO_COLUMN.put("DENOMINATION_CURRENCY", "i.denomination_currency");
        FIELD_TO_COLUMN.put("MATURITY_DATE", "i.maturity_date");
        FIELD_TO_COLUMN.put("DEBT_LIFETIME_IN_DAYS", "i.debt_lifetime_in_days");
        FIELD_TO_COLUMN.put("ACTIVE_FLAG", "i.active_flag");
        FIELD_TO_COLUMN.put("DATE_OPENED_IN_SIX", "i.date_opened_in_six");
        FIELD_TO_COLUMN.put("SUBSTITUTE_ISSUE_DATE", "i.substitute_issue_date");
        FIELD_TO_COLUMN.put("ISSUE_DATE", "i.issue_date");
        FIELD_TO_COLUMN.put("CAPITAL_CHANGE_DATE", "i.capital_change_date");
        FIELD_TO_COLUMN.put("SEDOL", "i.sedol");
        FIELD_TO_COLUMN.put("CUSIP", "i.cusip");
        FIELD_TO_COLUMN.put("CINS", "i.cins");
        FIELD_TO_COLUMN.put("AUSTRIAN", "i.austrian");
        FIELD_TO_COLUMN.put("BELGIAN", "i.belgian");
        FIELD_TO_COLUMN.put("CANADIAN", "i.canadian");
        FIELD_TO_COLUMN.put("GERMAN", "i.german");
        FIELD_TO_COLUMN.put("DENMARK", "i.denmark");
        FIELD_TO_COLUMN.put("FRANCE_RGA", "i.france_rga");
        FIELD_TO_COLUMN.put("FRANCE_EUROCLEAR", "i.france_euroclear");
        FIELD_TO_COLUMN.put("ITALIAN", "i.italian");
        FIELD_TO_COLUMN.put("JAPANESE_CURRENT", "i.japanese_current");
        FIELD_TO_COLUMN.put("JAPANESE_NEW", "i.japanese_new");
        FIELD_TO_COLUMN.put("LUXEMBOURG", "i.luxembourg");
        FIELD_TO_COLUMN.put("NETHERLAND", "i.netherland");
        FIELD_TO_COLUMN.put("NORWEGIAN", "i.norwegian");
        FIELD_TO_COLUMN.put("SWEDISH", "i.swedish");
        FIELD_TO_COLUMN.put("XS_INT_NUMBER", "i.xs_int_number");
        FIELD_TO_COLUMN.put("PORTUGAL", "i.portugal");
        FIELD_TO_COLUMN.put("SOUTH_KOREA", "i.south_korea");
        FIELD_TO_COLUMN.put("HONG_KONG", "i.hong_kong");
        FIELD_TO_COLUMN.put("FIGI_GLOBAL_ID", "i.figi_global_id");
        FIELD_TO_COLUMN.put("FIGI_GLOBAL_SHARE_CLASS_ID", "i.figi_global_share_class_id");
        FIELD_TO_COLUMN.put("OBLIGOR_ROLE", "i.obligor_role");
        FIELD_TO_COLUMN.put("UNDERLYING_OBLIGOR", "i.underlying_obligor");
        FIELD_TO_COLUMN.put("UNDERLYING_OBLIGOR_GK", "i.underlying_obligor_gk");
        FIELD_TO_COLUMN.put("REFERENCE_CAPITAL", "i.reference_capital");
        FIELD_TO_COLUMN.put("ACTUAL_CAPITAL", "i.actual_capital");
        FIELD_TO_COLUMN.put("REGIMES", "i.regimes");
        FIELD_TO_COLUMN.put("HOST_CH", "i.host_ch");
        FIELD_TO_COLUMN.put("HOST_ISIN", "i.host_isin");
        FIELD_TO_COLUMN.put("HOST_GK", "i.host_gk");
        FIELD_TO_COLUMN.put("HOST_ISSUER_SHORTNAME", "i.host_issuer_shortname");
        FIELD_TO_COLUMN.put("DESCRIPTION", "i.description");
        FIELD_TO_COLUMN.put("UNDERLYING_CH", "i.underlying_ch");
        FIELD_TO_COLUMN.put("UNDERLYING_ISIN", "i.underlying_isin");
        FIELD_TO_COLUMN.put("UNDERLYING_GK", "i.underlying_gk");
        FIELD_TO_COLUMN.put("UNDERLYING_ISSUER_SHORTNAME", "i.underlying_issuer_shortname");
        FIELD_TO_COLUMN.put("UNDERLYING_ISSUER_GK", "i.underlying_issuer_gk");
        FIELD_TO_COLUMN.put("UNDERLYING_ISSUER_NAME", "i.underlying_issuer_name");
        FIELD_TO_COLUMN.put("CH_OPTION", "i.ch_option");
        FIELD_TO_COLUMN.put("ISIN_OPTION", "i.isin_option");
        FIELD_TO_COLUMN.put("ISSUER_GK", "i.issuer_gk");
        FIELD_TO_COLUMN.put("ISSUER_NAME", "i.issuer_name");
        FIELD_TO_COLUMN.put("EXPIRY_DATE", "i.expiry_date");
        FIELD_TO_COLUMN.put("SETTLEMENT_STYLE", "i.settlement_style");
        FIELD_TO_COLUMN.put(REGIME, "t.regime");
        FIELD_TO_COLUMN.put("SANCTIONED", "t.sanctioned");
        FIELD_TO_COLUMN.put("TARGET", "t.target");
        FIELD_TO_COLUMN.put(LEGAL_BASIS, "t.legal_basis");
    }

    public String buildWhereClause(Set<SixSubFilters> sixSubFilters, MapSqlParameterSource paramSrc,
                                   String subqueryBase, String rootIsin, Set<String> emptyIsinForAllTargetsClauses,
                                   Set<String> containsIsinforAllTargetsClauses) {
        if (sixSubFilters == null || sixSubFilters.isEmpty()) {
            return "";
        }

        String finalWhereClause = "";
        List<String> andGroups = new ArrayList<>();

        for (SixSubFilters sub : sixSubFilters) {
            boolean forAllTargets = sub.getActiveSixSubORfilters().stream()
                    .anyMatch(or -> EMPTY_IN_ALL_TARGETS.equals(op(or)));
            boolean containsForAllTargets = sub.getActiveSixSubORfilters().stream()
                    .anyMatch(or -> CONTAINS_IN_ALL_TARGETS.equals(op(or)));
            if (forAllTargets) {
                frameAllTargetsQuery(sub, emptyIsinForAllTargetsClauses, paramSrc, subqueryBase, rootIsin);
            } else if (containsForAllTargets) {
                containsForAllTargetsQuery(sub, containsIsinforAllTargetsClauses, paramSrc, subqueryBase, rootIsin);
            } else {
                frameQuery(sub, andGroups, paramSrc, rootIsin, sixExclusionList);
            }
        }

        if (!andGroups.isEmpty()) {
            finalWhereClause = String.join(AND, andGroups);
        }

        return finalWhereClause;
    }

    public String buildBaseWhereClause(Set<SixSubFilters> sixSubFilters, MapSqlParameterSource paramSrc, String rootIsin) {
        if (sixSubFilters == null || sixSubFilters.isEmpty()) {
            return "";
        }
        List<String> andGroups = new ArrayList<>();
        for (SixSubFilters sub : sixSubFilters) {
            if (!isSelectionSubFilter(sub)) {
                frameQuery(sub, andGroups, paramSrc, rootIsin, sixExclusionList);
            }
        }
        return String.join(AND, andGroups);
    }

    public static boolean isSelectionSubFilter(SixSubFilters sub) {
        return sub.getActiveSixSubORfilters().stream().anyMatch(or -> isRegimeField(or)
                || EMPTY_IN_ALL_TARGETS.equals(op(or)) || CONTAINS_IN_ALL_TARGETS.equals(op(or)));
    }

    private Collection<String> containsForAllTargetsQuery(SixSubFilters sub,
                                                          Set<String> containsIsinforAllTargetsClauses,
                                                          MapSqlParameterSource paramSrc, String subqueryBase, String rootIsin) {
        List<String> regimeQuery = new ArrayList<>();
        String columnName = "";
        String paramNameUUIDRegime = "";
        String paramNameUUIDLegalBasis = "";

        for (SixSubORFilters or : sub.getActiveSixSubORfilters()) {
            String columnWithAlias = qualifiedColumn(or.getFieldName(), rootIsin);
            columnName = columnFor(or, columnWithAlias);
            paramNameUUIDRegime = paramName(REGIME);
            paramNameUUIDLegalBasis = paramName(LEGAL_BASIS);

            if (isRegimeField(or) && CONTAINS_IN_ALL_TARGETS.equals(op(or))) {
                regimeQuery.add(regimeConditionQuery(columnName, paramSrc, paramNameUUIDRegime, paramNameUUIDLegalBasis, or));
            }
        }

        if (!regimeQuery.isEmpty()) {
            String combinedRegimeQuery = (regimeQuery.size() == 1) ? regimeQuery.get(0) : "(" + String.join(OR, regimeQuery) + ")";
            containsIsinforAllTargetsClauses.addAll(containsForAllTargetsInClause(paramSrc, subqueryBase, rootIsin, combinedRegimeQuery));
        }

        return containsIsinforAllTargetsClauses;
    }

    private String regimeConditionQuery(String columnName, MapSqlParameterSource paramSrc, String paramNameUUIDRegime,
                                        String paramNameUUIDLegalBasis, SixSubORFilters or) {
        paramSrc.addValue(paramNameUUIDRegime, regimeName(or));
        paramSrc.addValue(paramNameUUIDLegalBasis, "%" + lowerValue(or) + "%");
        return columnName + " " + String.format(OPERATOR_SQL.get(EQUALS), paramNameUUIDRegime)
                + AND + textColumn(FIELD_TO_COLUMN.get(LEGAL_BASIS)) +
                " " + String.format(OPERATOR_SQL.get(NON_CONTAINS), paramNameUUIDLegalBasis);
    }

    private Collection<String> frameAllTargetsQuery(SixSubFilters sub, Set<String> excludeIsinForAllTargetsClauses,
                                                    MapSqlParameterSource paramSrc,
                                                    String subqueryBase, String rootIsin) {
        List<String> notEmptyClauses = new ArrayList<>();
        List<String> emptyClause = new ArrayList<>();

        for (SixSubORFilters or : sub.getActiveSixSubORfilters()) {
            qualifiedColumn(or.getFieldName(), rootIsin);
            if (isRegimeField(or) && EMPTY_IN_ALL_TARGETS.equals(op(or))) {
                if (flag(or)) {
                    emptyClause.add(regimeName(or));
                } else {
                    notEmptyClauses.add(regimeName(or));
                }
            }
        }

        String regimeColumn = textColumn(FIELD_TO_COLUMN.get(REGIME));
        if (!emptyClause.isEmpty()) {
            excludeIsinForAllTargetsClauses.addAll(emptyForAllTargetsInClause(regimeColumn, paramSrc, paramName(REGIME),
                    emptyClause, subqueryBase, rootIsin));
        }
        if (!notEmptyClauses.isEmpty()) {
            excludeIsinForAllTargetsClauses.addAll(notEmptyForAllTargetsInClause(regimeColumn, paramSrc, paramName(REGIME),
                    notEmptyClauses, subqueryBase, rootIsin));
        }

        return excludeIsinForAllTargetsClauses;
    }

    private List<String> frameQuery(SixSubFilters sub, List<String> andGroups, MapSqlParameterSource paramSrc, String rootIsin,
                                    String sixExclusionList) {
        List<String> orClauses = new ArrayList<>();
        for (SixSubORFilters or : sub.getActiveSixSubORfilters()) {
            String columnWithAlias = qualifiedColumn(or.getFieldName(), rootIsin);
            String columnName = columnFor(or, columnWithAlias);
            String paramNameUUIDLegalBasis = paramName(LEGAL_BASIS);
            String paramNameUUIDRegime = paramName(REGIME);

            if (isRegimeField(or)) {
                buildQueryForRegimes(columnName, paramNameUUIDRegime, paramNameUUIDLegalBasis, orClauses, paramSrc, or);
            } else {
                buildQueryForNonRegimes(columnName, rootIsin, orClauses, paramSrc, sixExclusionList, or, columnWithAlias);
            }
        }

        if (!orClauses.isEmpty()) {
            String orGroup = (orClauses.size() == 1) ? orClauses.get(0) : "(" + String.join(" OR ", orClauses) + ")";
            andGroups.add(orGroup);
        }
        return andGroups;
    }

    private List<String> emptyForAllTargetsInClause(String columnName, MapSqlParameterSource paramSrc, String paramNameUUIDRegime,
                                                    List<String> emptyClause, String subqueryBase, String rootIsin) {
        paramSrc.addValue(paramNameUUIDRegime, new ArrayList<>(emptyClause));
        String targetCondition = columnName + " " + String.format(OPERATOR_SQL.get("IN"), paramNameUUIDRegime);
        return allTargetsIsins(paramSrc, subqueryBase, rootIsin, targetCondition);
    }

    private List<String> notEmptyForAllTargetsInClause(String columnName, MapSqlParameterSource paramSrc, String paramNameUUIDRegime,
                                                       List<String> notEmptyClause, String subqueryBase, String rootIsin) {
        paramSrc.addValue(paramNameUUIDRegime, new ArrayList<>(notEmptyClause));   // one bind value per regime
        String targetCondition = columnName + " " + String.format(OPERATOR_SQL.get(NOT_IN), paramNameUUIDRegime)
                + AND + FIELD_TO_COLUMN.get(LEGAL_BASIS) + " " + NULL;
        return allTargetsIsins(paramSrc, subqueryBase, rootIsin, targetCondition);
    }

    private List<String> containsForAllTargetsInClause(MapSqlParameterSource paramSrc, String subqueryBase, String rootIsin,
                                                       String combinedRegimeQuery) {
        return allTargetsIsins(paramSrc, subqueryBase, rootIsin, combinedRegimeQuery);
    }

    private List<String> allTargetsIsins(MapSqlParameterSource paramSrc, String subqueryBase, String rootIsin,
                                         String targetCondition) {
        String isinColumn = defineColumn(rootIsin);
        String query = "SELECT /*+ LEADING(b u) USE_HASH(u) NO_MERGE(b) NO_MERGE(u) NO_PUSH_PRED(u) */ DISTINCT b." + isinColumn
                + " FROM (" + subqueryBase + ") b JOIN ("
                + ALL_TARGETS_INSTR + AND + targetCondition
                + ALL_TARGETS_STRUCT + AND + targetCondition
                + ALL_TARGETS_OPT + AND + targetCondition
                + ") u ON u.six_isin = b." + isinColumn;
        return jdbcTemplate.query(query, paramSrc, (rs, rowNum) -> rs.getString(isinColumn));
    }

    private List<String> buildQueryForRegimes(String columnName, String paramNameUUIDRegime, String paramNameUUIDLegalBasis,
                                              List<String> orClauses, MapSqlParameterSource paramSrc, SixSubORFilters or) {
        String opKey = op(or);
        String regimeIs = columnName + " " + String.format(OPERATOR_SQL.get(EQUALS), paramNameUUIDRegime);

        if (opKey.equals(IS_EMPTY) || opKey.equals(IS_NOT_EMPTY)) {
            boolean legalBasisEmpty = opKey.equals(IS_EMPTY) == flag(or);
            paramSrc.addValue(paramNameUUIDRegime, regimeName(or));
            orClauses.add("(" + regimeIs + AND + trimColumn(FIELD_TO_COLUMN.get(LEGAL_BASIS))
                    + (legalBasisEmpty ? NULL : NOT_NULL) + ")");

        } else if (opKey.equals(EQUALS) || opKey.equals(NOT_EQUALS)) {
            paramSrc.addValue(paramNameUUIDRegime, regimeName(or));
            paramSrc.addValue(paramNameUUIDLegalBasis, bindValue(or.getFilterValue()));
            orClauses.add("(" + regimeIs + AND +
                    getColumnsName(isNumeric(or.getFilterValue()), FIELD_TO_COLUMN.get(LEGAL_BASIS)) + " " +
                    String.format(OPERATOR_SQL.get(opKey), paramNameUUIDLegalBasis) + ")");

        } else if (opKey.equals(CONTAINS) || opKey.equals(NON_CONTAINS)) {
            paramSrc.addValue(paramNameUUIDRegime, regimeName(or));
            paramSrc.addValue(paramNameUUIDLegalBasis, "%" + lowerValue(or) + "%");
            orClauses.add("(" + regimeIs + AND + textColumn(FIELD_TO_COLUMN.get(LEGAL_BASIS)) + " " +
                    String.format(OPERATOR_SQL.get(opKey), paramNameUUIDLegalBasis) + ")");

        } else if (opKey.equals(STARTS_WITH) || opKey.equals(NOT_STARTS_WITH)) {
            paramSrc.addValue(paramNameUUIDRegime, regimeName(or));
            paramSrc.addValue(paramNameUUIDLegalBasis, lowerValue(or) + "%");
            orClauses.add("(" + regimeIs + AND + textColumn(FIELD_TO_COLUMN.get(LEGAL_BASIS)) + " " +
                    String.format(OPERATOR_SQL.get(opKey), paramNameUUIDLegalBasis) + ")");

        } else {
            throw new ReglissException("SIX filter: operator '" + opKey + "' is not supported for the regime field '"
                    + or.getFieldName() + "' - correct the filter");
        }

        return orClauses;
    }

    private List<String> buildQueryForNonRegimes(String columnName, String rootIsin, List<String> orClauses, MapSqlParameterSource paramSrc,
                                                 String sixExclusionList, SixSubORFilters or, String columnWithAlias) {
        String paramNameUUID = paramName(or.getFieldName());
        String opKey = op(or);

        switch (opKey) {
            case IS_EMPTY:                   return getQueryForIsEmpty(or, columnWithAlias, orClauses);
            case IS_NOT_EMPTY:               return getQueryForIsNotEmpty(or, columnWithAlias, orClauses);
            case CONTAINS:                   return getQueryForContains(paramSrc, or, textColumn(columnWithAlias), orClauses, paramNameUUID, opKey);
            case NON_CONTAINS:               return getQueryForContains(paramSrc, or, textColumn(columnWithAlias), orClauses, paramNameUUID, opKey);
            case STARTS_WITH:                return getQueryForStartsWith(paramSrc, or, textColumn(columnWithAlias), orClauses, paramNameUUID, opKey);
            case NOT_STARTS_WITH:            return getQueryForStartsWith(paramSrc, or, textColumn(columnWithAlias), orClauses, paramNameUUID, opKey);
            case "not included in exclusion":return getQueryForNotIncludedInExclusion(or, sixExclusionList, rootIsin, orClauses, paramSrc);
            case "included in exclusion":    return getQueryForIncludedInExclusion(or, sixExclusionList, rootIsin, orClauses, paramSrc);
            case EQUALS:                     return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            case NOT_EQUALS:                 return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            case "less than":                return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            case "greater than":             return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            case "less or equal":            return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            case "greater or equal":         return getQueryForCommonOperator(paramSrc, or, columnName, orClauses, paramNameUUID, opKey);
            default:
                throw new ReglissException("SIX filter: invalid operator '" + opKey + "' for the field '" + or.getFieldName() + "' - correct the filter");
        }
    }

    private List<String> getQueryForCommonOperator(MapSqlParameterSource paramSrc, SixSubORFilters or, String columnName,
                                                   List<String> orClauses, String paramNameUUID, String opKey) {
        paramSrc.addValue(paramNameUUID, bindValue(or.getFilterValue()));
        orClauses.add(columnName + " " + String.format(OPERATOR_SQL.get(opKey), paramNameUUID));

        return orClauses;
    }

    private List<String> getQueryForIncludedInExclusion(SixSubORFilters or, String sixExclusionList, String rootIsin,
                                                        List<String> orClauses, MapSqlParameterSource paramSrc) {
        boolean flag = flag(or);
        String clause = flag
                ? buildIncludedInClause(paramSrc, sixExclusionList)
                : buildNotIncludedInClause(rootIsin, paramSrc, sixExclusionList);
        orClauses.add(clause);

        return orClauses;
    }

    private List<String> getQueryForNotIncludedInExclusion(SixSubORFilters or, String sixExclusionList, String rootIsin, List<String> orClauses,
                                                           MapSqlParameterSource paramSrc) {
        boolean flag = flag(or);
        String clause = flag
                ? buildNotIncludedInClause(rootIsin, paramSrc, sixExclusionList)
                : buildIncludedInClause(paramSrc, sixExclusionList);
        orClauses.add(clause);

        return orClauses;
    }

    private List<String> getQueryForStartsWith(MapSqlParameterSource paramSrc, SixSubORFilters or, String columnName, List<String> orClauses,
                                               String paramNameUUID, String opKey) {
        paramSrc.addValue(paramNameUUID, lowerValue(or) + "%");
        orClauses.add(columnName + " " + String.format(OPERATOR_SQL.get(opKey), paramNameUUID));

        return orClauses;
    }

    private List<String> getQueryForContains(MapSqlParameterSource paramSrc, SixSubORFilters or, String columnName, List<String> orClauses,
                                             String paramNameUUID, String opKey) {
        paramSrc.addValue(paramNameUUID, "%" + lowerValue(or) + "%");
        orClauses.add(columnName + " " + String.format(OPERATOR_SQL.get(opKey), paramNameUUID));

        return orClauses;
    }

    private List<String> getQueryForIsNotEmpty(SixSubORFilters or, String columnWithAlias, List<String> orClauses) {
        boolean flag = flag(or);
        String clause = flag ? trimColumn(columnWithAlias) + NOT_NULL : trimColumn(columnWithAlias) + NULL;
        orClauses.add(clause);

        return orClauses;
    }

    private List<String> getQueryForIsEmpty(SixSubORFilters or, String columnWithAlias, List<String> orClauses) {
        boolean flag = flag(or);
        String clause = flag ? trimColumn(columnWithAlias) + NULL : trimColumn(columnWithAlias) + NOT_NULL;
        orClauses.add(clause);
        return orClauses;
    }

    private String getColumnsName(boolean numeric, String columnWithAlias) {
        return numeric ? "TO_NUMBER(" + columnWithAlias + " DEFAULT NULL ON CONVERSION ERROR)" : textColumn(columnWithAlias);
    }

    private String columnFor(SixSubORFilters or, String columnWithAlias) {
        if (isRegimeField(or)) {
            return textColumn(columnWithAlias);
        }
        return getColumnsName(isNumeric(or.getFilterValue()), columnWithAlias);
    }

    private String textColumn(String columnWithAlias) {
        return "LOWER(" + columnWithAlias + ")";
    }

    private static boolean isRegimeField(SixSubORFilters or) {
        return or.getFieldName() != null && or.getFieldName().endsWith(REGIME_FIELD);
    }

    private static String regimeName(SixSubORFilters or) {
        String field = or.getFieldName();
        return field.substring(0, field.trim().length() - REGIME_FIELD.length()).toLowerCase();
    }

    private static String op(SixSubORFilters or) {
        return or.getOperator() == null ? "" : or.getOperator().toLowerCase().trim();
    }

    private static String lowerValue(SixSubORFilters or) {
        return or.getFilterValue() == null ? "" : or.getFilterValue().toLowerCase();
    }

    private static boolean flag(SixSubORFilters or) {
        return Boolean.parseBoolean(lowerValue(or));
    }

    private Object bindValue(String filterValue) {
        if (isNumeric(filterValue)) {
            return new BigDecimal(filterValue.trim());
        }
        return filterValue == null ? "" : filterValue.toLowerCase();
    }

    private String qualifiedColumn(String uiField, String rootIsin) {
        String field = uiField.trim().toUpperCase(Locale.ROOT);
        if (field.endsWith(REGIME_FIELD)) {
            return FIELD_TO_COLUMN.get(REGIME);
        }
        SixFileKind kind = SixFileKind.ofRootIsin(rootIsin)
                .orElseThrow(() -> new ReglissException("Invalid root isin: " + rootIsin));
        if ("ISIN".equals(field)) {
            return kind.getRootIsinColumn();
        }
        if (KIND_COLUMNS.get(kind).contains(field)) {
            return FIELD_TO_COLUMN.getOrDefault(field, "i." + field.toLowerCase(Locale.ROOT));
        }
        if (TARGET_COLUMNS.contains(field)) {
            return FIELD_TO_COLUMN.getOrDefault(field, "t." + field.toLowerCase(Locale.ROOT));
        }
        throw new ReglissException("SIX filter field '" + uiField + "' is not a column of " + kind.getTable()
                + " or SIX_TARGET - check the filter definition");
    }

    private String paramName(String column) {
        return column.replaceAll("[^A-Z0-9]", "_") + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildNotIncludedInClause(String rootIsin, MapSqlParameterSource paramSrc, String sixExclusionList) {
        if (!exclusionListHasIsins(paramSrc, sixExclusionList)) {
            return "1 = 1";
        }
        return "(" + rootIsin + " IS NOT NULL AND " + EXCLUSION_ALIAS + ".code IS NULL)";
    }

    private String buildIncludedInClause(MapSqlParameterSource paramSrc, String sixExclusionList) {
        if (!exclusionListHasIsins(paramSrc, sixExclusionList)) {
            return "1 = 0";
        }
        return EXCLUSION_ALIAS + ".code IS NOT NULL";
    }

    private boolean exclusionListHasIsins(MapSqlParameterSource paramSrc, String sixExclusionList) {
        if (!paramSrc.hasValue(EXCLUSION_CHECKED_KEY)) {
            MapSqlParameterSource listParam = new MapSqlParameterSource(EXCLUSION_LIST_PARAM, sixExclusionList);
            Long isins = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + EXCLUSION_ISINS + ") x", listParam, Long.class);
            boolean hasIsins = isins != null && isins > 0;
            paramSrc.addValue(EXCLUSION_CHECKED_KEY, hasIsins);
            if (hasIsins) {
                paramSrc.addValue(EXCLUSION_LIST_PARAM, sixExclusionList);
            }
        }
        return Boolean.TRUE.equals(paramSrc.getValue(EXCLUSION_CHECKED_KEY));
    }

    public static boolean usesExclusionJoin(MapSqlParameterSource paramSrc) {
        return paramSrc.hasValue(EXCLUSION_LIST_PARAM);
    }

    public static String exclusionJoin(SixFileKind kind) {
        return " LEFT JOIN (SELECT DISTINCT e.code FROM (" + EXCLUSION_ISINS + ") e) " + EXCLUSION_ALIAS
                + " ON " + EXCLUSION_ALIAS + ".code = " + kind.getRootIsinColumn();
    }

    private boolean isNumeric(String s) { return s != null && s.matches("[-+]?\\d+(\\.\\d+)?"); }

    private String trimColumn(String columnName) { return "TRIM(" + columnName + ") "; }

    private String defineColumn(String rootIsin) {

        switch (rootIsin) {
            case "i.isin"         :    return "ISIN";
            case "i.host_isin"    :    return "HOST_ISIN";
            case "i.isin_option"  :    return "ISIN_OPTION";
            default:
                throw new ReglissException("Invalid root isin: " + rootIsin);
        }
    }

}

