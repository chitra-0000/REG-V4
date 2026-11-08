package com.bnpp.regliss.facade.dto.reference;

public class ReferentialDto implements Comparable<ReferentialDto>, ReferentialValue {
    public final Long id;
    public final String label;
    public final boolean v3Only;

    public ReferentialDto(Long id, String label, boolean v3Only) {
        this.id = id;
        this.label = label;
        this.v3Only = v3Only;
    }

    public String getLabel() { return label; }

    @Override
    public int compareTo(ReferentialDto o) { return label.compareTo(o.label); }

    @Override
    public Object getId() { return id; }

    public boolean isV3Only() { return v3Only; }
}

