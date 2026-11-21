package com.bnpp.regliss.entity.referential;

public enum DJSubfilterOperator {

    GREATER_THAN(">"),
    GREATER_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_OR_EQUAL("<=");

    private final String value;

    public String getValue() { return value; }

    private DJSubfilterOperator(String value) { this.value = value; }
}
