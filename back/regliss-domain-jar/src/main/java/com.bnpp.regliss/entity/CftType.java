package com.bnpp.regliss.entity;

import org.apache.commons.lang.StringUtils;

public enum CftType {
    PRIMARY,
    SECONDARY,
    NOT_DEFINED;

    public static CftType fromString(String string) {
        if (!StringUtils.isEmpty(string)) {
            for (CftType type : CftType.values()) {
                if (type.toString().equalsIgnoreCase(string)) {
                    return type;
                }
            }
        }

        return NOT_DEFINED;
    }

    public boolean isPrimary() { return this.equals(CftType.PRIMARY); }
}
