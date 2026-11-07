package com.bnpp.regliss.exporter;

import com.bnpp.regliss.entity.AddModSupAuditTrailFile;
import com.bnpp.regliss.entity.AddModSupExportFile;
import com.bnpp.regliss.entity.GeneratedFile;
import com.bnpp.regliss.entity.Version;
import com.bnpp.regliss.entity.referential.FileExportFormat;
import com.bnpp.regliss.exporter.writer.FileGenerationTry;
import com.bnpp.regliss.profile.ReglissBatchProfile;
import com.bnpp.regliss.repository.GeneratedFileRepository;
import com.bnpp.regliss.service.*;
import com.bnpp.regliss.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@ReglissBatchProfile
@Slf4j
public class Publisher {
    @Autowired
    private GeneratedFileRepository generatedFileRepo;

    @Autowired
    private FileGenerationNotificationService notificationService;

    @Autowired
    private ReglissRequestContext requestContext;

    @Autowired
    private ConcatenatedListAuditService concatenatedListAuditService;

    @Autowired
    private VersionService versionService;

    @Autowired
    private FileDeliveryService fileDeliveryService;

    @Autowired
    private GeneratedFileService generatedFileService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishFilesForVersion(long versionId, Map<GeneratedFile, FileGenerationTry> generatedFiles, GenerationReason reason) {
        generatedFileService.uploadGenerationResultInTx(generatedFiles);

        Set<GeneratedFile> generatedOk = MapUtils.filterValues(generatedFiles, FileGenerationTry::isOk).keySet();
        Set<GeneratedFile> generatedFailed = MapUtils.filterValues(generatedFiles, FileGenerationTry::hasErrors).keySet();

        log.debug("Generation of Version files finished: {} files OK, {} files FAILED", generatedOk.size(), generatedFailed.size());
        boolean onlyDeliveryReportIsGenerated = generatedFiles.keySet().stream()
                .noneMatch(gf -> gf.getGeneratedFileFormat().getFileFormat() != FileExportFormat.DELIVERY_REPORT);
        LocalDateTime publicationDate = requestContext.getRequestTime();
        if(onlyDeliveryReportIsGenerated) {
            versionService.publishFinishedDeliveryReportsTx(generatedOk);
        }else{
            versionService.publishFinishedForVersionInTx(versionId, publicationDate, generatedOk);
            log.info("Updated version {} status ",versionId);
        }

        notificationService.sendEmailsAfterPublicationInTx(MapUtils.filterKeys(generatedFiles, GeneratedFile::isKO).keySet(), versionId);
        fileDeliveryService.scheduleDeliveriesInTx(generatedOk, publicationDate, reason);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishAddModSup(AddModSupExportFile fileToGenerate, File file) {
        generatedFileService.updateAddModSupOnSuccessInTx(fileToGenerate, file);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishAddModSupAuditTrail(AddModSupAuditTrailFile fileToGenerate, File file, Optional<Version> optionalVersion) {
        generatedFileService.updateAddModSupAuditTrailOnSuccessInTx(fileToGenerate, file, optionalVersion);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void publishFilesForConcatenatedList(Map<GeneratedFile, FileGenerationTry> generatedFiles, GenerationReason reason) {
        generatedFiles = MapUtils.mapKeys(generatedFiles, detached -> generatedFileRepo.getExactlyOne(detached.getId()));
        generatedFileService.uploadGenerationResultInTx(generatedFiles);

        Set<GeneratedFile> generatedOk = MapUtils.filterValues(generatedFiles, FileGenerationTry::isOk).keySet();
        Set<GeneratedFile> generatedFailed = MapUtils.filterValues(generatedFiles, FileGenerationTry::hasErrors).keySet();

        log.debug("Generation of OFAC files finished: {} files OK, {} files FAILED", generatedOk.size(), generatedFailed.size());

        generatedOk.forEach(concatenatedListAuditService::publishOneGeneratedFileInTx);

        fileDeliveryService.scheduleDeliveriesInTx(generatedOk, requestContext.getRequestTime(), reason);
    }

}

