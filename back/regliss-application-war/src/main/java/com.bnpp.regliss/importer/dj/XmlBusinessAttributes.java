package com.bnpp.regliss.importer.dj;

public enum XmlBusinessAttributes {

    LOW_QUALITY_AKA("Low Quality AKA"),
    RECLASSIFIED_AKA("Reclassified AKA"),
    SINGLE_STRING_NAME("SingleStringName"),
    ORIGINAL_SCRIPT_NAME("OriginalScriptName"),
    CORE_AKA("Name"),
    WATCHLIST_SPELLING_VARIATION("Spelling Variation"),
    WATCHLIST_FORMERLY_KNOWN_AS("Formerly Known As"),
    WATCHLIST_EXPANDED_LANGUAGE_VARIATION("Expanded Language Variation"),
    WATCHLIST_ALSO_KNOWN_AS("Also Known As"),
    WATCHLIST_MAIDEN_NAME("Maiden Name");

    private final String value;

    public String getValue() { return value; }

    private XmlBusinessAttributes(String value) { this.value = value; }
}

