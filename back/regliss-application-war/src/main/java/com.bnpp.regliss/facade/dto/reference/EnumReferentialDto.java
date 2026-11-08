package com.bnpp.regliss.facade.dto.reference;


public class EnumReferentialDto implements Comparable<EnumReferentialDto>,ReferentialValue {
    public String id;
    public String label;
    public EnumReferentialDto() {
    }
    public EnumReferentialDto(String id, String label) {
        this.id = id;
        this.label = label;
    }
    @Override
    public int compareTo(EnumReferentialDto o) { return label.compareTo(o.label); }
    @Override
    public Object getId() { return id; }

    @Override
    public String getLabel() { return label; }

}

