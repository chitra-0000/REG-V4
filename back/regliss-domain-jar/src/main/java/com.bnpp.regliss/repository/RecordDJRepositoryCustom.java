package com.bnpp.regliss.repository;

import java.util.Map;

public interface RecordDJRepositoryCustom  {

    void batchUpdateCsvSnippets(Map<Long, String> recordDJIdToCsvSnippet);
}

