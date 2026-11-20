package com.bnpp.regliss.importer.manual;

import com.bnpp.regliss.MappingColumn;
import com.bnpp.regliss.annotation.AuditedFieldCode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class EntityFieldIndexes {

    private final Map<AuditedFieldCode, List<Integer>> fieldColumnIndexes;

    public EntityFieldIndexes(List<MappingColumn> columns) {
        this.fieldColumnIndexes = columns.stream()
                .collect(groupingBy(MappingColumn::getFieldCode,
                        mapping(MappingColumn::getColumnIndex, toList())));
    }

    public int getSingleIndexForField(AuditedFieldCode fieldCode) {
        List<Integer> indexes = fieldColumnIndexes.get(fieldCode);
        if (indexes == null || indexes.size() != 1) {
            throw new IllegalStateException("Expected exactly one column for field " + fieldCode);
        }
        return indexes.get(0);
    }

    public String getSingleColumnValue(AuditedFieldCode fieldCode, List<String> row) {
        List<Integer> indexes = fieldColumnIndexes.get(fieldCode);
        if (indexes == null || indexes.size() != 1) {
            return null;
        }
        String value = row.get(indexes.get(0));
        return org.apache.commons.lang.StringUtils.isBlank(value) ? null : value;
    }

    public String getJoinedColumnValues(AuditedFieldCode fieldCode, List<String> row) {
        List<Integer> indexes = fieldColumnIndexes.get(fieldCode);
        if (indexes == null) {
            return null;
        }
        String joined = indexes.stream()
                .map(row::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("|"));
        return joined.isEmpty() ? null : joined;
    }

    public List<String> getAllColumnValues(AuditedFieldCode fieldCode, List<String> row) {
        List<Integer> index = fieldColumnIndexes.get(fieldCode);
        if (index == null) {
            return emptyList();
        }
        return index.stream()
                .map(row::get)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public Set<AuditedFieldCode> getFieldCodes() {
        return unmodifiableSet(fieldColumnIndexes.keySet());
    }
}
