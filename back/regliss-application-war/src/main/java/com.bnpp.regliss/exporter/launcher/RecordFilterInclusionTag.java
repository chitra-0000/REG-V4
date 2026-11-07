package com.bnpp.regliss.exporter.launcher;

import lombok.AllArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@AllArgsConstructor(access = PRIVATE)
public class RecordFilterInclusionTag {
    public final static RecordFilterInclusionTag OTHER = new RecordFilterInclusionTag("OTHER", TagType.OTHER);
    private String filterTag;
    private TagType type;

    public static RecordFilterInclusionTag includedFilterTag(String filterTagName) {
        return new RecordFilterInclusionTag(filterTagName, TagType.INCLUDED);
    }
    public static RecordFilterInclusionTag excludedFilterTag(String filterTagName) {
        return new RecordFilterInclusionTag(filterTagName, TagType.EXCLUDED);
    }

    public String getExportValue() { return type.exportValue(filterTag); }

    private enum TagType {
        INCLUDED {
            @Override
            String exportValue(String tagValue) { return String.format("I_%s", tagValue); }
        },
        EXCLUDED {
            @Override
            String exportValue(String tagValue) { return String.format("E_%s", tagValue); }
        },
        OTHER {
            @Override
            String exportValue(String tagValue) { return tagValue; }
        };

        abstract String exportValue(String tagValue);
    }
}
