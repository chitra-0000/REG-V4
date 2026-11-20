package com.bnpp.regliss.importer.manual;

// [MISSING FROM SCREENSHOTS: lines 3-14 of the original file were not visible in the photos]

public class EntityFieldIndexes {
    // It's a List of indexes only for these AuditedFieldCode :
    /** @see com.bnpp.regliss.importer.manual.MappingFileParser.ALLOWED_DUPLICATED_FIELDS */
    private final Map<AuditedFieldCode, List<Integer>> fieldColumnIndexes;

    public EntityFieldIndexes(List<MappingColumn> columns) {
        fieldColumnIndexes = columns.stream()
                .collect(groupingBy(MappingColumn::getFieldCode, mapping(MappingColumn::getColumnIndex, toList())));
    }

    public Integer getSingleIndexForField(AuditedFieldCode fieldCode) {
        List<Integer> indexes = fieldColumnIndexes.get(fieldCode);
        if (indexes == null) {
            throw new IllegalArgumentException("AuditedFieldCode not found: " + fieldCode);
        }
        if (indexes.size() != 1) {
            throw new IllegalArgumentException("AuditedFieldCode found multiple times: " + fieldCode);
        }
        return indexes.get(0);
    }

    public String getSingleColumnValue(AuditedFieldCode fieldCode, List<String> row) {
        List<Integer> indexes = fieldColumnIndexes.getOrDefault(fieldCode, emptyList());
        if (indexes.size()!=1) {
            return null;
        }
        String columnValueStr = row.get(indexes.get(0));
        if (StringUtils.isBlank(columnValueStr)) {
            return null;
        } else {
            return columnValueStr;
        }
    }

    public String getJoinedColumnValues(AuditedFieldCode fieldCode, List<String> row) {
        List<String> allColumnValues = getAllColumnValues(fieldCode, row);
        if (allColumnValues.isEmpty()) {
            return null;
        }
        String columnValuesStr = String.join("|", allColumnValues);
        if (columnValuesStr.isEmpty()) {
            return null;
        } else {
            return columnValuesStr;
        }
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

