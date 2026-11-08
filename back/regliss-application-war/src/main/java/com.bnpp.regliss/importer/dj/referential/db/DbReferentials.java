package com.bnpp.regliss.importer.dj.referential.db;

// [MISSING FROM SCREENSHOTS: lines 3-11 of the original file were not visible in the photos]

public class DbReferentials {

    private final Map<DJReferentialType, Map<String, Long>> referentials = new EnumMap<>(DJReferentialType.class);

    public DbReferentials() { Stream.of(DJReferentialType.values()).forEach(e -> referentials.put(e, new HashMap<>())); }

    public Map<String, Long> get(DJReferentialType refType) { return referentials.get(refType); }

    public void set(DJReferentialType recordCategory, Map<String, Long> allValues) {
        referentials.put(recordCategory, allValues);
    }

    @Override
    public String toString() {
        return referentials.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().size())).toString();
    }
}

