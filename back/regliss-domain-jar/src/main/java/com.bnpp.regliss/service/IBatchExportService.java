package com.bnpp.regliss.service;

import java.util.List;

public interface IBatchExportService {
    void persistConcatenatedListExportGenerationInTx(List<Long> generatedFileIds, GenerationReason reason);
}
