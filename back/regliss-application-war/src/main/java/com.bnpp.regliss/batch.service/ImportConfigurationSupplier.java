package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.ImportFileType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ImportConfigurationSupplier {

    @Value("${import.alow.incremental.for.DOW_JONES_CORE}")
    private boolean allowDJCoreIncremental;
    @Value("${import.alow.incremental.for.DOW_JONES_WATCHLIST}")
    private boolean allowDJWatchlistIncremental;
    @Value("${import.alow.incremental.for.DOW_JONES_AME}")
    private boolean allowDJAmeIncremental;
    @Value("${import.alow.incremental.for.DOW_JONES_WL_EX}")
    private boolean allowDJWatchlistExtraIncremental;

    public boolean isIncrementalAllowedForDJFormat(ImportFileType importFiletype){
        switch(importFiletype){
            case DOW_JONES_CORE:
                return allowDJCoreIncremental;
            case DOW_JONES_AME:
                return allowDJAmeIncremental;
            case DOW_JONES_WATCHLIST:
                return allowDJWatchlistIncremental;
            case DOW_JONES_WATCHLIST_EXTRA:
                return allowDJWatchlistExtraIncremental;
            default:
                return false;
        }

    }

}
