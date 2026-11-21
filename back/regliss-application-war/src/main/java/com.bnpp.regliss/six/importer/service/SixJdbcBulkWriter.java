package com.bnpp.regliss.six.importer.service;

import com.bnpp.regliss.profile.ReglissBatchProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import com.bnpp.regliss.utils.EntityUtils;

@Component
@ReglissBatchProfile
@Slf4j
public class SixJdbcBulkWriter {

    private static final Pattern SQL_NAME = Pattern.compile("^[A-Z][A-Z0-9_$#]{0,127}$");

    private static final int JDBC_CHUNK = 1000;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Environment environment;

    private TransactionTemplate newTx;

    private final Map<Class<?>, TableMapping> mappings = new ConcurrentHashMap<>();
    private final Map<String, String> idSequences = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        newTx = new TransactionTemplate(transactionManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <P> int insertBatch(List<P> parents, long listId, long versionId,
                               Function<P, ? extends Collection<?>> childrenOf, String childFkColumn) {
        if (parents.isEmpty()) {
            return 0;
        }
        final TableMapping parentMap = mappingFor(parents.get(0).getClass());
        final List<Object> children = new ArrayList<>();
        final List<P> childParent = new ArrayList<>();
        collectChildren(parents, childrenOf, children, childParent);

        final TableMapping childMap = children.isEmpty() ? null : mappingFor(children.get(0).getClass());
        final String parentSeq = idSequenceFor(parentMap.table);
        final String childSeq = childMap == null ? null : idSequenceFor(childMap.table);

        parents.forEach(EntityUtils::truncateStringFields);
        children.forEach(EntityUtils::truncateStringFields);

        Integer written = newTx.execute(status -> {
            List<Long> parentIds = nextIds(parentSeq, parents.size());
            Map<Object, Long> idByParent = new IdentityHashMap<>(parents.size() * 2);
            for (int i = 0; i < parents.size(); i++) {
                parentMap.setId(parents.get(i), parentIds.get(i));
                idByParent.put(parents.get(i), parentIds.get(i));
            }
            batchInsert(parentMap, parents, parentIds, (row, col) -> listOrVersionId(col, listId, versionId));

            if (childMap != null) {
                List<Long> childIds = nextIds(childSeq, children.size());
                List<Long> fk = new ArrayList<>(children.size());
                for (int i = 0; i < children.size(); i++) {
                    childMap.setId(children.get(i), childIds.get(i));
                    fk.add(idByParent.get(childParent.get(i)));
                }
                batchInsert(childMap, children, childIds, (row, col) -> childFkColumn.equals(col) ? fk.get(row) : null);
            }
            return children.size();
        });
        return written == null ? 0 : written;
    }

    private static <P> void collectChildren(List<P> parents, Function<P, ? extends Collection<?>> childrenOf,
                                            List<Object> children, List<P> childParent) {
        for (P parent : parents) {
            Collection<?> kids = childrenOf.apply(parent);
            if (kids == null) {
                continue;
            }
            for (Object kid : kids) {
                if (kid != null) {
                    children.add(kid);
                    childParent.add(parent);
                }
            }
        }
    }

    private static Object listOrVersionId(String column, long listId, long versionId) {
        if ("LIST_ID".equals(column)) {
            return listId;
        }
        if ("VERSION_ID".equals(column)) {
            return versionId;
        }
        return null;
    }

    public int deleteVersion(Class<?> parentEntity, long versionId) {
        String sql = "DELETE FROM " + mappingFor(parentEntity).table + " WHERE VERSION_ID = ?";
        Integer n = newTx.execute(status -> jdbcTemplate.update(sql, versionId));
        return n == null ? 0 : n;
    }

    private interface RowValues {
        Object valueFor(int row, String column);
    }

    private void batchInsert(TableMapping map, List<?> rows, List<Long> ids, RowValues forced) {
        for (int from = 0; from < rows.size(); from += JDBC_CHUNK) {
            final int start = from;
            final int end = Math.min(from + JDBC_CHUNK, rows.size());
            jdbcTemplate.batchUpdate(map.insertSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int row = start + i;
                    Object entity = rows.get(row);
                    ps.setLong(1, ids.get(row));
                    int p = 2;
                    for (ColumnMapping c : map.columns) {
                        Object value = forced.valueFor(row, c.column);
                        if (value != null) {
                            ps.setLong(p++, ((Number) value).longValue());
                        } else {
                            c.bind(ps, p++, entity);
                        }
                    }
                }

                @Override
                public int getBatchSize() {
                    return end - start;
                }
            });
        }
    }

    private List<Long> nextIds(String sequence, int count) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT " + sequence + ".NEXTVAL FROM DUAL CONNECT BY LEVEL <= ?", Long.class, count);
        if (ids.size() != count) {
            throw new IllegalStateException(sequence + " returned " + ids.size() + " ids, expected " + count);
        }
        return ids;
    }

    private String idSequenceFor(String table) {
        return idSequences.computeIfAbsent(table, t -> {
            String override = environment.getProperty("six.import.id-sequence." + t);
            if (override != null && !override.trim().isEmpty()) {
                return qualified(override.trim().toUpperCase(Locale.ROOT));
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT OWNER, SEQUENCE_NAME FROM ALL_TAB_IDENTITY_COLS" +
                            " WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') AND TABLE_NAME = ? AND COLUMN_NAME = 'ID'", t);
            if (rows.size() != 1) {
                throw new IllegalStateException("No identity sequence found for " + t + ".ID in the current schema;"
                        + " set six.import.id-sequence." + t + "=OWNER.SEQUENCE_NAME");
            }
            return qualified(rows.get(0).get("OWNER") + "." + rows.get(0).get("SEQUENCE_NAME"));
        });
    }

    private static String qualified(String ownerDotName) {
        String[] parts = ownerDotName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!SQL_NAME.matcher(part).matches()) {
                throw new IllegalArgumentException("Bad sequence name: " + ownerDotName);
            }
            sb.append(sb.length() == 0 ? "" : ".").append('"').append(part).append('"');
        }
        return sb.toString();
    }

    private TableMapping mappingFor(Class<?> type) {
        return mappings.computeIfAbsent(type, TableMapping::new);
    }

    static final class TableMapping {
        final String table;
        final Method idSetter;
        final List<ColumnMapping> columns = new ArrayList<>();
        final String insertSql;

        TableMapping(Class<?> type) {
            this.table = tableName(type);
            Method idSet = null;
            for (Class<?> c : hierarchyTopDown(type)) {
                for (Field f : c.getDeclaredFields()) {
                    if (!isPersistent(f)) {
                        continue;
                    }
                    if (f.isAnnotationPresent(Id.class)) {
                        idSet = publicMethod(type, accessorName("set", f), f.getType());
                    } else {
                        ColumnMapping column = columnFor(type, c, f, table);
                        if (column != null) {
                            columns.add(column);
                        }
                    }
                }
            }
            if (idSet == null) {
                throw new IllegalStateException(type.getName() + " has no @Id field");
            }
            this.idSetter = idSet;
            this.insertSql = insertSql(table, columns);
        }

        void setId(Object entity, Long id) {
            try {
                idSetter.invoke(entity, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot set the id of " + entity.getClass().getSimpleName(), e);
            }
        }

        private static String tableName(Class<?> type) {
            Table t = type.getAnnotation(Table.class);
            if (t == null || t.name().isEmpty()) {
                throw new IllegalStateException(type.getName() + " has no @Table(name=...)");
            }
            return sqlName(t.name(), type.getName());
        }

        private static List<Class<?>> hierarchyTopDown(Class<?> type) {
            List<Class<?>> hierarchy = new ArrayList<>();
            for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
                hierarchy.add(c);
            }
            Collections.reverse(hierarchy);
            return hierarchy;
        }

        private static boolean isPersistent(Field f) {
            int mod = f.getModifiers();
            return !(Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic()
                    || f.isAnnotationPresent(Transient.class)
                    || f.isAnnotationPresent(OneToMany.class)
                    || f.isAnnotationPresent(ManyToMany.class));
        }

        private static ColumnMapping columnFor(Class<?> type, Class<?> declaring, Field f, String table) {
            Column col = f.getAnnotation(Column.class);
            JoinColumn join = f.getAnnotation(JoinColumn.class);
            boolean relation = f.isAnnotationPresent(ManyToOne.class) || f.isAnnotationPresent(OneToOne.class);
            if (col != null) {
                return col.insertable()
                        ? new ColumnMapping(sqlName(columnName(col.name(), f), f.getName()), f, readerFor(type, f), false)
                        : null;
            }
            if (relation && join != null) {
                return join.insertable()
                        ? new ColumnMapping(sqlName(columnName(join.name(), f), f.getName()), f, readerFor(type, f), true)
                        : null;
            }
            throw new IllegalStateException("Cannot map " + declaring.getSimpleName() + "." + f.getName()
                    + " for insert into " + table + ": add @Column(name=...) or @Transient");
        }

        private static Method readerFor(Class<?> type, Field f) {
            String prefix = f.getType() == boolean.class ? "is" : "get";
            return publicMethod(type, accessorName(prefix, f));
        }

        private static String insertSql(String table, List<ColumnMapping> columns) {
            StringBuilder cols = new StringBuilder("ID");
            StringBuilder qs = new StringBuilder("?");
            for (ColumnMapping c : columns) {
                cols.append(", ").append(c.column);
                qs.append(", ?");
            }
            return "INSERT INTO " + table + " (" + cols + ") VALUES (" + qs + ")";
        }

        private static String columnName(String annotated, Field f) {
            return (annotated == null || annotated.isEmpty()) ? f.getName() : annotated;
        }

        private static String sqlName(String raw, String where) {
            String n = raw.replace("\"", "").toUpperCase(Locale.ROOT);
            if (!SQL_NAME.matcher(n).matches()) {
                throw new IllegalStateException("Unsupported SQL name '" + n + "' (" + where + ")");
            }
            return n;
        }

        private static String accessorName(String prefix, Field f) {
            String n = f.getName();
            return prefix + Character.toUpperCase(n.charAt(0)) + n.substring(1);
        }
    }

    private static Method publicMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(type.getSimpleName() + " has no public " + name + "(): add @Getter/@Setter", e);
        }
    }

    static final class ColumnMapping {
        final String column;
        final Class<?> type;
        final Method getter;
        final Method relatedIdGetter;
        final EnumType enumType;

        ColumnMapping(String column, Field field, Method getter, boolean foreignKey) {
            this.column = column;
            this.type = field.getType();
            this.getter = getter;
            this.relatedIdGetter = foreignKey ? publicMethod(field.getType(), "getId") : null;
            Enumerated en = field.getAnnotation(Enumerated.class);
            this.enumType = en == null ? EnumType.ORDINAL : en.value();
        }

        void bind(PreparedStatement ps, int pos, Object entity) throws SQLException {
            Object value = read(getter, entity, column);
            if (relatedIdGetter != null) {
                bindForeignKey(ps, pos, value);
            } else {
                bindValue(ps, pos, value);
            }
        }

        private void bindForeignKey(PreparedStatement ps, int pos, Object related) throws SQLException {
            Object id = related == null ? null : read(relatedIdGetter, related, column);
            if (id == null) {
                ps.setNull(pos, Types.NUMERIC);
            } else {
                ps.setLong(pos, ((Number) id).longValue());
            }
        }

        private void bindValue(PreparedStatement ps, int pos, Object value) throws SQLException {
            if (value == null) {
                ps.setNull(pos, sqlType(type));
            } else if (value instanceof String) {
                ps.setString(pos, (String) value);
            } else if (value instanceof Enum) {
                bindEnum(ps, pos, (Enum<?>) value);
            } else if (value instanceof Boolean) {
                ps.setInt(pos, Boolean.TRUE.equals(value) ? 1 : 0);
            } else if (value instanceof java.util.Date && !(value instanceof java.sql.Date)) {
                ps.setTimestamp(pos, new Timestamp(((java.util.Date) value).getTime()));
            } else {
                ps.setObject(pos, value);
            }
        }

        private void bindEnum(PreparedStatement ps, int pos, Enum<?> e) throws SQLException {
            if (enumType == EnumType.STRING) {
                ps.setString(pos, e.name());
            } else {
                ps.setInt(pos, e.ordinal());
            }
        }

        private static Object read(Method getter, Object target, String column) throws SQLException {
            try {
                return getter.invoke(target);
            } catch (ReflectiveOperationException e) {
                throw new SQLException("Cannot read " + getter.getName() + " for column " + column, e);
            }
        }

        private static int sqlType(Class<?> t) {
            if (t == String.class || t.isEnum()) {
                return Types.VARCHAR;
            }
            if (Number.class.isAssignableFrom(t) || t == Boolean.class) {
                return Types.NUMERIC;
            }
            if (java.util.Date.class.isAssignableFrom(t) || t.getName().startsWith("java.time.")) {
                return Types.TIMESTAMP;
            }
            return Types.VARCHAR;
        }
    }
}
