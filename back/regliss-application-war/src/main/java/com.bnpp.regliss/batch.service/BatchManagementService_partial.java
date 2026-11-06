package com.bnpp.regliss.batch.service;

import com.bnpp.regliss.entity.BatchJobType;
import com.bnpp.regliss.entity.ImportedFile;
import com.bnpp.regliss.entity.ReglissList;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.batch.BatchExport;
import com.bnpp.regliss.entity.batch.BatchJobExecution;
import com.bnpp.regliss.exporter.BatchExportService;
import com.bnpp.regliss.repository.VersionRepository;
import com.bnpp.regliss.repository.batch.BatchJobExecutionRepository;
import com.bnpp.regliss.service.ReferenceService;
import com.bnpp.regliss.service.ReglissRequestContext;
import com.bnpp.regliss.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// [GAP] lines 25 to ~143 not in the photos (class header, fields and earlier methods)

    private Long createNewBatchJobExecutionInTx(BatchJobType batchJobType, BatchJobParameterLoggable parameters, Version version,
                                                ReglissList reglissList) {
        BatchJobExecution batchJobExecution = new BatchJobExecution(nodeDetailsSupplier.getNodeId(), nodeDetailsSupplier.getHostName(),
                batchJobType, parameters);
        if (version != null) {
            batchJobExecution.setVersion(version);
            batchJobExecution.setReglissListId(version.getList().getId());
        }

        if (reglissList != null) {
            batchJobExecution.setReglissListId(reglissList.getId());
        }

        return batchJobExecutionRepository.save(batchJobExecution).getId();
    }

    private boolean hasImportAutoInProgress(String nodeId) {
        return !(batchJobExecutionRepository.findByNodeIdAndJobTypeInAndEndDateIsNull(nodeId, Arrays.asList(BatchJobType.BATCH_IMPORT_AUTO,
                BatchJobType.BATCH_IMPORT_SIXRAW)).isEmpty());
    }

    private boolean otherAliveBatchNodesHaveImportAutoInProgress() {
        List<String> aliveBatchNodeIds = heartbeatService.getAllActiveBatchNodeIds();
        aliveBatchNodeIds.removeIf(e -> e.equals(nodeDetailsSupplier.getNodeId()));
        return aliveBatchNodeIds.stream().allMatch(this::hasImportAutoInProgress);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createNewSixRawAutoBatchExecutionInTx(AutomaticImportFeedBase feed, ReglissList reglissList) {
        BatchImportAutoLoggedParameters feedParams = new BatchImportAutoLoggedParameters(reglissList.getId(), feed);
        return createNewBatchJobExecutionInTx(BatchJobType.BATCH_IMPORT_SIXRAW, feedParams, null, reglissList);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createNewSixRawExportBatchExecutionInTx(BatchExport batchExport, Version version) {
        batchExportService.deleteProcessedBatchExport(batchExport.getId());
        return createNewBatchJobExecutionInTx(BatchJobType.BATCH_EXPORT_SIXRAW, new BatchExportLoggedParameters(batchExport.getExportParams()),
                version, null);
    }
}
