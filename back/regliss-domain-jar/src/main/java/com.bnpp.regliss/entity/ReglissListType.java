package com.bnpp.regliss.entity;

public enum ReglissListType {
    REGULATORY("Regulatory"), //old SANCTIONS
    INTERNAL_POLICY("Internal Policy"), //old SURVEILLANCE
    DERIVATIVE("Derivative"); //old GEOGRAPHIC

    private final String label;

    ReglissListType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
