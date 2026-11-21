package com.bnpp.regliss.entity.referential;

public enum Confidentiality{
    PUBLIC (0, "Public"),
    RESTRICTED (1, "Restricted"),
    CONFIDENTIAL (2, "Confidential");

    private int priority;
    private String label;

    Confidentiality(int priority, String label){
        this.priority = priority;
        this.label = label;
    }

    public int getPriority() { return priority; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return this.name(); }

}
