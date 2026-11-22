package com.bnpp.regliss.vo;

import lombok.Getter;

@Getter
public class TruncateStringFieldsVO {
    private final String fieldName;
    private final String originalValue;
    private final String truncatedValue;

    public TruncateStringFieldsVO(String fieldName, String originalValue, String truncatedValue) {
        this.fieldName = fieldName;
        this.originalValue = originalValue;
        this.truncatedValue = truncatedValue;
    }
}
