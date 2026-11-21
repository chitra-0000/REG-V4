package com.bnpp.regliss.entity.record;

public enum VerificationStatus {
    OK, KO;

    public static VerificationStatus fromValue(String v) { return valueOf(v); }
}
