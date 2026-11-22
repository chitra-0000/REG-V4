package com.bnpp.regliss.vo.export.dj.watchlist;

import lombok.Value;

@Value
public class RecordDjExportData {
    private String exportSnippet;
    // this is either externalUuid if public list or reglissReference if confidential list
    private String urlReference;
    private Long id;
}
