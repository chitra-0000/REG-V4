package com.bnpp.regliss.importer.dj.referential.xml;

// [MISSING FROM SCREENSHOTS: lines 3-9 of the original file were not visible in the photos]

public class DJXmlReferentials {

    private EnumMap<DJReferentialType, Set<DJXmlReferential>> referentials = new EnumMap<>(DJReferentialType.class);
    private String fileDate;

    public DJXmlReferentials() {
        Stream.of(DJReferentialType.values()).forEach(e -> referentials.put(e, new HashSet<>()));
    }

    public Set<DJXmlReferential> get(DJReferentialType refType) { return referentials.get(refType); }



    public void set(DJReferentialType recordCategory, Set<DJXmlReferential> allValues) {
        referentials.put(recordCategory, allValues);
    }

    @Override
    public String toString() {
        return referentials.entrySet().stream().collect(toMap(Entry::getKey, e -> e.getValue().size())).toString();
    }





}

