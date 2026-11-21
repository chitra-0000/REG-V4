package com.bnpp.regliss.entity;

public enum VersionStatus {
    DRAFT,
    /** = INTEGRATED for automatic versions*/
    VALIDATED,
    RETURNED,
    DELETED,
    /** = CONFIRMED for automatic versions*/
    CERTIFIED,
    PUBLISHED_OK,
    PUBLISHED_KO,
    DELIVERED_OK,
    DELIVERED_KO
}
