package com.bnpp.regliss.entity;

import lombok.Getter;

public enum BatchJobType {
    BATCH_EXPORT("Batch Export"),
    BATCH_IMPORT_SEMIAUTO("Batch Import SemiAuto"),
    BATCH_IMPORT_AUTO("Batch Import Auto"),
    BATCH_IMPORT_SIXRAW("Batch Import Six Raw"),
    BATCH_EXPORT_SIXRAW("Batch Export Six Raw");

    @Getter
    private String prettyName;

    BatchJobType(String prettyName) { this.prettyName = prettyName; }
}
