package com.bnpp.regliss.vo;

import java.util.stream.Stream;

public enum DJFileImportMode {
    FULL("F"),
    INCREMENTAL("I");

    private final String importFileNameSuffix;

    private DJFileImportMode(String importFileNameSuffix) {
        this.importFileNameSuffix = importFileNameSuffix;
    }

    public static DJFileImportMode fromImportFileNameSuffix(String suffixCharacter) {
        return Stream.of(values())
                .filter(e -> e.importFileNameSuffix.equalsIgnoreCase(suffixCharacter))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not a valid file name mode suffix: " + suffixCharacter));
    }

    public String toImportFileNameSuffix() {
        return importFileNameSuffix;
    }

}